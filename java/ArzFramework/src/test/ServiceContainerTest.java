import com.arz_x.CommonException;
import com.arz_x.CommonResultCode;
import com.arz_x.common.helpers.Contract;
import com.arz_x.common.service_container.InstantiateConstructor;
import com.arz_x.common.service_container.OptionalLogic;
import com.arz_x.common.service_container.ServiceContainer;
import com.arz_x.common.service_container.ServiceInitializationType;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Created by Rihter on 01.12.2015.
 * Unit tests for Service container
 */
public class ServiceContainerTest {
    //region Declarations
    public static class DummyNonConstructableEmptyClass {}

    public static class DummyEmptyClass {
        @InstantiateConstructor
        public DummyEmptyClass() {}
    }

    public interface IDummyInterface {}

    public interface IDummyInterface2 {}

    public static class DummyInterfaceImplementation implements IDummyInterface {
        @InstantiateConstructor
        public DummyInterfaceImplementation() {}
    }

    public static class DummyInterfaceImplementation2 implements IDummyInterface {
        private IDummyInterface2 parameter;

        @InstantiateConstructor
        public DummyInterfaceImplementation2(IDummyInterface2 dummyInterface2) {
            this.parameter = dummyInterface2;
        }

        public IDummyInterface2 getParameter() {
            return this.parameter;
        }
    }

    public static class DummyInterface2Implementation implements IDummyInterface2 {
        private IDummyInterface parameter;

        @InstantiateConstructor
        public DummyInterface2Implementation(@OptionalLogic IDummyInterface dummyInterface) {
            this.parameter = dummyInterface;
        }

        public IDummyInterface getParameter() {
            return this.parameter;
        }
    }

    public static class DummyInterface2Implementation2 implements IDummyInterface2 {
        @InstantiateConstructor
        public DummyInterface2Implementation2(IDummyInterface dummyInterface) {
            Contract.unusedVariable(dummyInterface);
        }
    }

    public static class DummyCompoundClass {
        @InstantiateConstructor
        public DummyCompoundClass(IDummyInterface obj1, DummyEmptyClass obj2, DummyNonConstructableEmptyClass obj3) {
            Contract.requireNotNull(obj1, obj2, obj3);
        }
    }

    public static class DummyCompoundClassWithOptional {
        @InstantiateConstructor
        public DummyCompoundClassWithOptional(@OptionalLogic IDummyInterface obj1
                , DummyEmptyClass obj2
                , @OptionalLogic DummyNonConstructableEmptyClass obj3) {
            Contract.unusedVariable(obj1, obj2, obj3);
        }
    }

    public static class ConstructionCalculusClass {
        private static int constructorInvocationCounter = 0;

        @InstantiateConstructor
        public ConstructionCalculusClass() {
            ++ConstructionCalculusClass.constructorInvocationCounter;
        }

        public static void clearConstructorInvocationCounter(){
            ConstructionCalculusClass.constructorInvocationCounter = 0;
        }

        public static int getConstructorInvocationCounter(){
            return ConstructionCalculusClass.constructorInvocationCounter;
        }
    }
    //endregion

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldReturnInitialService() {
        //region Initialization
        final DummyNonConstructableEmptyClass dummy = new DummyNonConstructableEmptyClass();

        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.initializationType = ServiceInitializationType.Lazy;
        settings.instantiatedObjects = new Object[] {
                        dummy,
                };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        {
            final Object resultObject = serviceContainer.getService(dummy.getClass().getName());
            Assert.assertEquals(dummy, resultObject);
        }

        {
            final DummyNonConstructableEmptyClass resultObject = serviceContainer.getService(DummyNonConstructableEmptyClass.class);
            Assert.assertEquals(dummy, resultObject);
        }
        //endregion
    }

    @Test
    public void shouldReturnInitialServiceByInterface() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.initializationType = ServiceInitializationType.Lazy;
        settings.instantiatedObjects = new Object[] {
                new DummyInterfaceImplementation(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        {
            final Object resultInterface = serviceContainer.getService(IDummyInterface.class.getName());
            Assert.assertTrue(resultInterface instanceof IDummyInterface);
            Assert.assertTrue(resultInterface instanceof DummyInterfaceImplementation);
        }

        {
            final IDummyInterface resultInterface = serviceContainer.getService(IDummyInterface.class);
            Assert.assertTrue(resultInterface instanceof DummyInterfaceImplementation);
        }
        //endregion
    }

    @Test
    public void shouldCreateAndReturnNewService() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.serviceClasses = new String[]{
                DummyInterfaceImplementation.class.getName(),
        };
        settings.initializationType = ServiceInitializationType.Lazy;

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        {
            final Object resultObject = serviceContainer.getService(DummyInterfaceImplementation.class.getName());
            Assert.assertTrue(resultObject instanceof DummyInterfaceImplementation);
        }

        {
            final DummyInterfaceImplementation resultObject = serviceContainer.getService(DummyInterfaceImplementation.class);
            Assert.assertNotNull(resultObject);
        }
        //endregion
    }

    @Test
    public void shouldCreateAndReturnNewServiceByInterface() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.serviceClasses = new String[] {
                DummyInterfaceImplementation.class.getName(),
        };
        settings.initializationType = ServiceInitializationType.Lazy;

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        {
            final Object resultInterface = serviceContainer.getService(IDummyInterface.class.getName());
            Assert.assertTrue(resultInterface instanceof IDummyInterface);
            Assert.assertTrue(resultInterface instanceof DummyInterfaceImplementation);
        }

        {
            final IDummyInterface resultInterface = serviceContainer.getService(IDummyInterface.class);
            Assert.assertTrue(resultInterface instanceof DummyInterfaceImplementation);
        }
        //endregion
    }

    @Test
    public void shouldCacheServiceUsingStringParamGetServiceMethod() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.initializationType = ServiceInitializationType.Lazy;
        settings.serviceClasses = new String[] {
                ConstructionCalculusClass.class.getName(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        ConstructionCalculusClass.clearConstructorInvocationCounter();

        Assert.assertNotNull(serviceContainer.getService(ConstructionCalculusClass.class.getName()));
        Assert.assertNotNull(serviceContainer.getService(ConstructionCalculusClass.class.getName()));

        Assert.assertEquals(1, ConstructionCalculusClass.getConstructorInvocationCounter());
        //endregion
    }

    @Test
    public void shouldCacheServiceUsingClassParameterGetServiceMethod() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.initializationType = ServiceInitializationType.Lazy;
        settings.serviceClasses = new String[] {
                ConstructionCalculusClass.class.getName(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        ConstructionCalculusClass.clearConstructorInvocationCounter();

        Assert.assertNotNull(serviceContainer.getService(ConstructionCalculusClass.class));
        Assert.assertNotNull(serviceContainer.getService(ConstructionCalculusClass.class));

        Assert.assertEquals(1, ConstructionCalculusClass.getConstructorInvocationCounter());
        //endregion
    }

    @Test
    public void shouldCreateServiceOnFirstAccessUsingLazyInitialization() {
        //region Initialization
        ConstructionCalculusClass.clearConstructorInvocationCounter();

        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.initializationType = ServiceInitializationType.Lazy;
        settings.serviceClasses = new String[] {
                ConstructionCalculusClass.class.getName(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        Assert.assertEquals(0, ConstructionCalculusClass.getConstructorInvocationCounter());
        Assert.assertNotNull(serviceContainer.getService(ConstructionCalculusClass.class));
        Assert.assertEquals(1, ConstructionCalculusClass.getConstructorInvocationCounter());
        //endregion
    }

    @Test
    public void shouldCreateServiceWhileServiceContainerConstructionUsingAllAtOnceInitialization() {
        //region Initialization
        ConstructionCalculusClass.clearConstructorInvocationCounter();

        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.initializationType = ServiceInitializationType.CreateAllAtStart;
        settings.serviceClasses = new String[] {
                ConstructionCalculusClass.class.getName(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        Assert.assertEquals(1, ConstructionCalculusClass.getConstructorInvocationCounter());
        Assert.assertNotNull(serviceContainer.getService(ConstructionCalculusClass.class));
        Assert.assertEquals(1, ConstructionCalculusClass.getConstructorInvocationCounter());
        //endregion
    }

    @Test
    public void shouldCreateServiceDependingOnOtherServices() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.initializationType = ServiceInitializationType.Lazy;
        settings.serviceClasses = new String[] {
                DummyCompoundClass.class.getName(),
                DummyInterfaceImplementation.class.getName(),
                DummyEmptyClass.class.getName(),
        };
        settings.instantiatedObjects = new Object[] {
                new DummyNonConstructableEmptyClass(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Testing
        {
            final Object dummyCompoundClass = serviceContainer.getService(DummyCompoundClass.class.getName());
            Assert.assertTrue(dummyCompoundClass instanceof DummyCompoundClass);
        }

        {
            final DummyCompoundClass dummyCompoundClass = serviceContainer.getService(DummyCompoundClass.class);
            Assert.assertNotNull(dummyCompoundClass);
        }
        //endregion
    }

    @Test
    public void shouldCreateServiceIfOptionalDependencyIsNotResolved() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.serviceClasses = new String[] {
                DummyCompoundClassWithOptional.class.getName(),
                DummyEmptyClass.class.getName(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        {
            final Object resultObject = serviceContainer.getService(DummyCompoundClassWithOptional.class.getName());
            Assert.assertTrue(resultObject instanceof DummyCompoundClassWithOptional);
        }

        {
            final DummyCompoundClassWithOptional resultObject = serviceContainer.getService(DummyCompoundClassWithOptional.class);
            Assert.assertNotNull(resultObject);
        }
        //endregion
    }

    @Test
    public void shouldCreateServiceIfCircularReferenceWithOptional() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.serviceClasses = new String[] {
                DummyInterfaceImplementation2.class.getName(),
                DummyInterface2Implementation.class.getName(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        {
            final Object resultObject1 = serviceContainer.getService(DummyInterfaceImplementation2.class.getName());
            final Object resultObject2 = serviceContainer.getService(DummyInterface2Implementation.class.getName());

            Assert.assertTrue(resultObject1 instanceof DummyInterfaceImplementation2);
            Assert.assertTrue(resultObject2 instanceof DummyInterface2Implementation);

            Assert.assertNotNull(((DummyInterfaceImplementation2)resultObject1).getParameter());
            Assert.assertNull(((DummyInterface2Implementation)resultObject2).getParameter());
        }

        {
            final DummyInterfaceImplementation2 resultObject1 = serviceContainer.getService(DummyInterfaceImplementation2.class);
            final DummyInterface2Implementation resultObject2 = serviceContainer.getService(DummyInterface2Implementation.class);

            Assert.assertNotNull(resultObject1.getParameter());
            Assert.assertNull(resultObject2.getParameter());
        }
        //endregion
    }

    @Test
    public void shouldThrowNotFoundExceptionIfServiceNotRegistered() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        //region Test
        thrown.expect(new CustomTypeSafeMatcher<CommonException>("Must throw exception") {
                          public boolean matchesSafely(CommonException exp) {
                              return exp.getResultCode() == CommonResultCode.NotFound;
                          }
                      }
        );
        serviceContainer.getService(DummyEmptyClass.class);
        //endregion Test
    }

    @Test
    public void shouldReturnNullIfDependencyNotFound() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.serviceClasses = new String[] {
                "ThisClassDoesNotExist1122!!::"
        };
        //endregion

        thrown.expect(new CustomTypeSafeMatcher<CommonException>("Must throw exception") {
                          public boolean matchesSafely(CommonException exp) {
                              return exp.getResultCode() == CommonResultCode.NotFound;
                          }
                      }
        );

        new ServiceContainer(settings);
    }

    @Test
    public void shouldReturnNullIfCircularDependencies() {
        //region Initialization
        ServiceContainer.Settings settings = new ServiceContainer.Settings();
        settings.serviceClasses = new String[] {
                DummyInterfaceImplementation2.class.getName(),
                DummyInterface2Implementation2.class.getName(),
        };

        ServiceContainer serviceContainer = new ServiceContainer(settings);
        //endregion

        thrown.expect(new CustomTypeSafeMatcher<CommonException>("Must throw exception") {
            public boolean matchesSafely(CommonException exp) {
                Contract.unusedVariable(exp);
                return true;
            }
        });

        serviceContainer.getService(DummyInterfaceImplementation2.class);
    }
}
