package com.arz_x.common.service_container;

import com.arz_x.CommonException;
import com.arz_x.CommonResultCode;
import com.arz_x.ResultCodeException;
import com.arz_x.common.helpers.Contract;

import java.lang.reflect.*;

import java.util.*;

/**
 * Created by Rihter on 22.11.2015.
 * Lightweight realization of IOC template through constructor injection.
 * For more complex scenarios please use Google Juice Injector
 */
public class ServiceContainer {
    public static class Settings {
        public ServiceInitializationType initializationType;

        /* Target amount of operated services.
         * Each string must be a full class name  */
        public String[] serviceClasses;

        /* Already instantiated classes for component usage */
        public Object[] instantiatedObjects;

        public Settings() {
            this.initializationType = ServiceInitializationType.Lazy;
        }
    }

    private class ObjectContainer {
        public ObjectContainer(Object service) {
            Objects.requireNonNull(service);
            this.service = service;
        }

        public Object getService() {
            return this.service;
        }

        private final Object service;
    }

    private static int MAX_CLASS_CREATION_DEPTH = 100;

    private Set<String> allowedServiceClasses;
    private Map<String, Class<?>> interfaceClassMap;
    private Map<String, ObjectContainer> cacheServiceMap;

    public ServiceContainer(Settings settings) {
        Contract.requireNotNull(settings);
        Contract.requireNotNull(settings.initializationType);

        this.cacheServiceMap = new HashMap<>();
        this.allowedServiceClasses = new HashSet<>();
        this.interfaceClassMap = new HashMap<>();

        if (settings.serviceClasses != null) {
            this.allowedServiceClasses.addAll(Arrays.asList(settings.serviceClasses));


            for (String serviceClassName : settings.serviceClasses) {
                try {
                    if (this.interfaceClassMap.containsKey(serviceClassName))
                        continue;
                    saveAllClassInterfaceMappings(Class.forName(serviceClassName));
                } catch (ClassNotFoundException exp) {
                    throw new CommonException(CommonResultCode.NotFound
                            , String.format("Class '%s' is not found", serviceClassName));
                }
            }

            if (settings.initializationType == ServiceInitializationType.CreateAllAtStart) {
                for (String serviceClassName : settings.serviceClasses) {
                    if (getService(serviceClassName) == null)
                        throw new CommonException(CommonResultCode.NotFound
                                , String.format("Class '%s\' is not found", serviceClassName));
                }
            }
        }

        if (settings.instantiatedObjects != null) {
            for (Object obj : settings.instantiatedObjects) {
                saveAllClassInterfaceMappings(obj.getClass());
                cacheServiceMap.put(obj.getClass().getName(), new ObjectContainer(obj));
            }
        }
    }

    public synchronized  <TargetObject> TargetObject getService(Class<TargetObject> objectClass){
        try {
            Objects.requireNonNull(objectClass);

            Class<?> searchedClass = objectClass;
            if (objectClass.isInterface()) {
                final Class<?> implementationClassObject = this.interfaceClassMap.get(objectClass.getName());
                if (implementationClassObject == null)
                    throw new CommonException(CommonResultCode.NotFound
                            , String.format("No class implementation for interface '%s' is registered", objectClass.getName()));
                searchedClass = implementationClassObject;
            }
            return (TargetObject)getInternalService(searchedClass, false, true, null, 0);
        } catch (ResultCodeException exp) {
            throw exp;
        } catch (Exception exp) {
            throw new CommonException(CommonResultCode.AssertError);
        }
    }

    public synchronized Object getService(String fullClassName) {
        Contract.requireNotNull(fullClassName);

        final Class<?> implementationClass = this.interfaceClassMap.get(fullClassName);
        if (implementationClass == null) {
            throw new CommonException(CommonResultCode.NotFound
                    , String.format("No class implementation for '%s' is registered", fullClassName));
        }
        return getService(implementationClass);
    }

    private Object getInternalService(Class<?> searchedClassArg, boolean isOptional
            , boolean cacheResult, Set<String> creatingObjects, int searchDepth)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (searchedClassArg == null)
            throw new CommonException(CommonResultCode.AssertError);

        if (searchDepth > MAX_CLASS_CREATION_DEPTH)
            return throwIfNeeded(isOptional, new CommonException(CommonResultCode.InvalidState, "Max searched depth was reached"));

        final String searchedClassFullName = searchedClassArg.getName();

        {
            Object resultObject = searchInCacheAndCheckForCreation(searchedClassFullName);
            if (resultObject != null)
                return resultObject;
        }

        // after a while we may not even get here! Make all allocations as lazy as possible
        if (creatingObjects == null)
            creatingObjects = new HashSet<>();

        if (creatingObjects.contains(searchedClassFullName))
            return throwIfNeeded(isOptional, new CommonException(CommonResultCode.InvalidState
                    , String.format("Circular reference found with class \"%s\"", searchedClassFullName)));

        Constructor<?> matchedConstructor = null;
        {
            Constructor<?>[] classConstructors = searchedClassArg.getConstructors();
            for (Constructor<?> iteratorConstructor : classConstructors) {
                if (iteratorConstructor.getDeclaredAnnotation(InstantiateConstructor.class) != null) {
                    matchedConstructor = iteratorConstructor;
                    break;
                }
            }

            if (matchedConstructor == null)
                return throwIfNeeded(isOptional, new CommonException(CommonResultCode.NotFound
                        , String.format("Unable to find InstanceConstructor for \"%s\"", searchedClassFullName)));
        }
        creatingObjects.add(searchedClassFullName);

        final Parameter[] constructorParameters = matchedConstructor.getParameters();
        final Object[] inputArguments = new Object[constructorParameters.length];
        for (int parameterIndex = 0; parameterIndex < constructorParameters.length; ++parameterIndex) {
            final String parameterClassName = constructorParameters[parameterIndex].getType().getName();
            final boolean isOptionalArgument = constructorParameters[parameterIndex].getAnnotation(OptionalLogic.class) != null;

            Class<?> argumentClassObject = this.interfaceClassMap.get(parameterClassName);
            if (argumentClassObject == null) {
                inputArguments[parameterIndex] = throwIfNeeded(isOptionalArgument
                        , new CommonException(CommonResultCode.NotFound
                                , String.format("Unable to get class implementation for '%s' interface", parameterClassName)));
            }
            else {
                inputArguments[parameterIndex] = getInternalService(argumentClassObject
                        , isOptionalArgument
                        , cacheResult
                        , creatingObjects
                        , searchDepth + 1);
            }
        }

        final ObjectContainer objectContainer = new ObjectContainer(matchedConstructor.newInstance(inputArguments));
        if (cacheResult)
            this.cacheServiceMap.put(searchedClassFullName, objectContainer);

        creatingObjects.remove(searchedClassFullName);
        return objectContainer.getService();
    }

    private Object searchInCacheAndCheckForCreation(String fullClassName) {
        final ObjectContainer objectIterator = this.cacheServiceMap.get(fullClassName);
        if (objectIterator != null) {
            final Object resultService = objectIterator.getService();
            if (!resultService.getClass().getName().equals(fullClassName))
                return new CommonException(CommonResultCode.AssertError);
            return resultService;
        }

        if (!isAllowedServiceCreation(fullClassName))
            throw new CommonException(CommonResultCode.NotFound, String.format("Not allowed to create :\"%s\"", fullClassName));
        return null;
    }

    private void saveAllClassInterfaceMappings(Class<?> serviceClass) {
        final String serviceClassName = serviceClass.getName();
        this.interfaceClassMap.put(serviceClassName, serviceClass);

        List<Class<?>> classInterfaces = new ArrayList<>();
        getAllClassInterfaces(serviceClass, classInterfaces);

        // Starting from java 1.8 interfaces can inherit each other, so there may be repeats
        Set<String> classInterfacesNames = new HashSet<>();
        for (Class<?> classInterface : classInterfaces)
            classInterfacesNames.add(classInterface.getName());

        for (String interfaceName : classInterfacesNames) {
            final Class<?> previousClassRealization = this.interfaceClassMap.put(interfaceName, serviceClass);
            if (previousClassRealization != null)
                throw new CommonException(CommonResultCode.InvalidState
                        , String.format("Classes '%s' and '%s' both realize same interface '%s'"
                        , previousClassRealization.getName()
                        , serviceClassName
                        , interfaceName));
        }
    }

    private boolean isAllowedServiceCreation(String serviceName) {
        return (this.allowedServiceClasses == null) || this.allowedServiceClasses.contains(serviceName);
    }

    private static void getAllClassInterfaces(Class<?> classObject, List<Class<?>> resultInterfaces) {
        final Class<?>[] classInterfaces = classObject.getInterfaces();
        for (Class<?> classInterface : classInterfaces) {
            resultInterfaces.add(classInterface);
            getAllClassInterfaces(classInterface, resultInterfaces);
        }
    }

    private static Object throwIfNeeded(boolean isOptional, RuntimeException exp) {
        if (!isOptional)
            throw exp;
        return null;
    }
}
