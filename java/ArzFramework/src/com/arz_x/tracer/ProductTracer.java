package com.arz_x.tracer;

import com.arz_x.CommonException;
import com.arz_x.CommonResultCode;
import com.arz_x.common.helpers.Contract;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * Created by Rihter on 27.04.2015.
 * Represents a helper for managing product traces with lifecycle
 *
 * Base concept:
 * - put all trace files to the same folder and limit its size
 * - limit size of the trace files
 * - delete old trace files
 * - reopen the same trace file
 */
public class ProductTracer implements ITracer, Closeable {

    public static class Settings {
        public static final long DEFAULT_SIZE = 64 * 1024L;
        public static final long UNLIMITED_SIZE = -1;

        public Settings() {
            this.doAlwaysStartNewTraceFile = false;
            this.maxTraceFilesSumSize = DEFAULT_SIZE;
        }

        public Settings(boolean doAlwaysStartNewTraceFile
                , long maxTraceFilesSumSize) {
            this.doAlwaysStartNewTraceFile = doAlwaysStartNewTraceFile;
            this.maxTraceFilesSumSize = maxTraceFilesSumSize;
        }

        /*If true - always creates new file for traces*/
        public boolean doAlwaysStartNewTraceFile;

        /*Maximum size of all trace files*/
        public long maxTraceFilesSumSize;
    }

    private static final String PROCESSING_FILE_TRACE_PREFIX = "_progress_";
    private static final String TRACE_FILE_PREFIX = "trace_";
    private static final String TRACE_FILE_EXTENSION = ".log";
    private static final String TRACE_FILE_NAME_PATTERN = PROCESSING_FILE_TRACE_PREFIX + "%d.%d.%d_%d:%d:%d:%d" + TRACE_FILE_EXTENSION;

    private String tracesDirectory;
    private IFileTracer fileTracer;
    private IProductTracerEvents productTracerEvents;

    private volatile long maxTraceFilesSumSize;
    private volatile long finishedTraceFilesSumSize;

    private SortedSet<File> finishedTraceFiles;

    public static String[] getAllTraceFiles(String tracesDirectory) {
        return getTraceFiles(tracesDirectory, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return (filename.startsWith(TRACE_FILE_PREFIX) || filename.startsWith(PROCESSING_FILE_TRACE_PREFIX)) && filename.endsWith(TRACE_FILE_EXTENSION);
            }
        });
    }

    /**
     * Constructs new ProductTracer that manages tracer lifecycle
     * @param tracesDirectory Path where trace files will be stored. Creates new if not exists already
     * @param tracer Tracer that will be used for tracing new files
     * @param settings Configuration settings for ProductTracer
     */
    public ProductTracer(String tracesDirectory
            , IFileTracer tracer
            , Settings settings
            , IProductTracerEvents productTracerEvents) {
        Contract.requireStringNotNullOrEmpty(tracesDirectory);
        Contract.requireNotNull(tracer);
        Contract.requireNotNull(settings);

        final Comparator<File> fromOlderToNewerDateOrderComparator = new Comparator<File>() {
            @Override
            public int compare(File st1, File st2) {
                return getDatePartForTraceFile(st1.getName()).compareTo(getDatePartForTraceFile(st2.getName()));
            }
        };

        this.tracesDirectory = tracesDirectory;
        this.fileTracer = tracer;
        this.maxTraceFilesSumSize = settings.maxTraceFilesSumSize;
        this.productTracerEvents = productTracerEvents;
        this.finishedTraceFiles = new TreeSet<>(fromOlderToNewerDateOrderComparator);

        final File tracesDirectoryObject = new File(this.tracesDirectory);
        if ((!tracesDirectoryObject.mkdirs()) && (!tracesDirectoryObject.isDirectory()))
            throw new CommonException(CommonResultCode.InvalidParameter, "Incorrect tracesDirectory argument");

        SortedSet<File> allTraceFiles = new TreeSet<>(Collections.reverseOrder(fromOlderToNewerDateOrderComparator));
        {
            final File[] allDirectoryFiles = new File(this.tracesDirectory).listFiles();
            if (allDirectoryFiles != null) {
                for (File file : allDirectoryFiles) {
                    final String filename = file.getName();
                    if (!filename.endsWith(TRACE_FILE_EXTENSION))
                        continue;

                    if ((filename.startsWith(TRACE_FILE_PREFIX)) || (filename.startsWith(PROCESSING_FILE_TRACE_PREFIX)))
                        allTraceFiles.add(file);
                }
            }
        }

        allTraceFiles.removeAll(checkDiskSpaceQuota(allTraceFiles));

        if ((!settings.doAlwaysStartNewTraceFile) && (!allTraceFiles.isEmpty()) && (isProcessingFile(allTraceFiles.first()))) {
            final File processingFile = allTraceFiles.first();
            this.fileTracer.openExistingFile(processingFile.getAbsolutePath());
            allTraceFiles.remove(processingFile);
        }
        else {
            openNewTraceFile();
        }

        for (File traceFile : allTraceFiles) {
            File file = traceFile;
            if (isProcessingFile(file))
                file = finishTraceFile(traceFile);

            this.finishedTraceFiles.add(file);
            this.finishedTraceFilesSumSize += file.length();
        }

        checkDiskSpaceQuota();
    }

    public synchronized void traceMessage(TraceLevel traceLevel, String message) {
        if (this.fileTracer == null)
            return;

        this.fileTracer.traceMessage(traceLevel, message);
        if (this.finishedTraceFilesSumSize + this.fileTracer.getFileSize() > this.maxTraceFilesSumSize)
            checkDiskSpaceQuota();
    }

    private void internalClose() {
        if (this.fileTracer instanceof Closeable) {
            try {
                ((Closeable) this.fileTracer).close();
            }
            catch (IOException exp) {
                // this might never happen
            }
        }
        this.fileTracer = null;
    }

    public synchronized void close() throws IOException {
        internalClose();
    }

    /**
     * Pauses tracing
     * Further tracing will start in the last trace file
     */
    public synchronized void pause() {
        internalClose();
    }

    /**
     * Finishes tracing
     * Further tracing will start in the new trace file
     */
    public synchronized void finish() {
        if (this.fileTracer != null) {
            finishTraceFile(new File(this.fileTracer.getCurrentFilePath()));
            internalClose();
        }
    }

    private void openNewTraceFile() {
        this.fileTracer.openNewFile(new File(this.tracesDirectory, createTraceFileName()).getAbsolutePath());
    }

    private void checkDiskSpaceQuota() {
        if (this.maxTraceFilesSumSize == Settings.UNLIMITED_SIZE)
            return;

        long currentTraceFilesSize = this.finishedTraceFilesSumSize + this.fileTracer.getFileSize();

        List<File> removedFiles = new ArrayList<>();

        Iterator<File> existingFilesIterator = this.finishedTraceFiles.iterator();
        while ((existingFilesIterator.hasNext()) && (currentTraceFilesSize > this.maxTraceFilesSumSize)) {
            final File currentFile = existingFilesIterator.next();
            removedFiles.add(currentFile);
            currentTraceFilesSize -= currentFile.length();
        }

        // our current trace file is still larger than the limit
        if (currentTraceFilesSize > this.maxTraceFilesSumSize) {
            removedFiles.add(new File(this.fileTracer.getCurrentFilePath()));
            openNewTraceFile();
        }

        this.finishedTraceFilesSumSize = currentTraceFilesSize;

        if ((productTracerEvents != null) && (!removedFiles.isEmpty())) {
            File[] rmFiles = new File[removedFiles.size()];
            removedFiles.toArray(rmFiles);
            productTracerEvents.onRemoveFiles(rmFiles);
        }

        for (File f : removedFiles)
            f.delete(); // we should just ignore the result

        this.finishedTraceFiles.removeAll(removedFiles);
    }

    /**
     * Verifies disk quota according to specified set of files sorted from more important to least important
     * @param files Initial set of files
     * @return Files that didn't match disk quota and have been removed
     */
    private List<File> checkDiskSpaceQuota(SortedSet<File> files) {
        Contract.requireNotNull(files);

        if ((this.maxTraceFilesSumSize == Settings.UNLIMITED_SIZE) || (files.isEmpty()))
            return new ArrayList<>();

        List<File> removedFiles = new ArrayList<>();
        {
            long currentDiskQuota = 0;
            for (File processingFile : files) {
                currentDiskQuota += processingFile.length();

                // Note: even if equal delete -> the first trace should delete this file anyway
                if (currentDiskQuota >= this.maxTraceFilesSumSize) {
                    removedFiles.add(processingFile);
                }
            }
        }

        if (this.productTracerEvents != null) {
            File[] rmFiles = new File[removedFiles.size()];
            removedFiles.toArray(rmFiles);
            productTracerEvents.onRemoveFiles(rmFiles);
        }

        for (File fileToRemove : removedFiles)
            fileToRemove.delete(); // we just can't do anything. Fail silently

        return removedFiles;
    }

    private static String[] getTraceFiles(String tracesDirectory, FilenameFilter filenameFilter) {
        File[] traceFiles = new File(tracesDirectory).listFiles(filenameFilter);
        if (traceFiles == null)
            return null;

        String[] resultFileNames = new String[traceFiles.length];
        for (int fileIndex = 0; fileIndex < resultFileNames.length; ++fileIndex)
            resultFileNames[fileIndex] = traceFiles[fileIndex].getAbsolutePath();

        return resultFileNames;
    }

    private static boolean isProcessingFile(File file) {
        Contract.requireNotNull(file);
        return file.getName().startsWith(PROCESSING_FILE_TRACE_PREFIX);
    }

    private static File finishTraceFile(File traceFile) throws CommonException {
        final String newTraceFileName = TRACE_FILE_PREFIX + traceFile.getName().substring(PROCESSING_FILE_TRACE_PREFIX.length());
        final File finishedTraceFile = new File(traceFile.getParent(), newTraceFileName);

        if (!traceFile.renameTo(finishedTraceFile))
            throw new CommonException(CommonResultCode.AccessIsDenied);

        return finishedTraceFile;
    }

    private static String getDatePartForTraceFile(String filename) {
        // better than loop
        if (filename.startsWith(TRACE_FILE_PREFIX))
            return filename.substring(TRACE_FILE_PREFIX.length(), filename.length() - TRACE_FILE_PREFIX.length());

        if (filename.startsWith(PROCESSING_FILE_TRACE_PREFIX))
            return filename.substring(PROCESSING_FILE_TRACE_PREFIX.length(), filename.length() - PROCESSING_FILE_TRACE_PREFIX.length());

        throw new CommonException(CommonResultCode.UnExpected);
    }

    private static String createTraceFileName() {
        final Calendar currentTime = new GregorianCalendar();
        return String.format(TRACE_FILE_NAME_PATTERN,
                currentTime.get(Calendar.YEAR),
                currentTime.get(Calendar.MONTH),
                currentTime.get(Calendar.DAY_OF_MONTH),
                currentTime.get(Calendar.HOUR_OF_DAY),
                currentTime.get(Calendar.MINUTE),
                currentTime.get(Calendar.SECOND),
                currentTime.get(Calendar.MILLISECOND));
    }
}
