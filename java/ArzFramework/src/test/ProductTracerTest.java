import com.arz_x.common.helpers.Contract;
import com.arz_x.tracer.IFileTracer;
import com.arz_x.tracer.IProductTracerEvents;
import com.arz_x.tracer.ProductTracer;
import com.arz_x.tracer.TraceLevel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

/**
 * Created by Rihter on 07.01.2016.
 * Tests the ProductTracer class
 */
public class ProductTracerTest {
    private static final String DUMMY_TRACE_MESSAGE = "dummy_trace_message";
    private static final TraceLevel DUMMY_TRACE_LEVEL = TraceLevel.Info;

    private static final File TEMP_FOLDER = new File("temp");

    @Before
    public void setUp() {
        TEMP_FOLDER.mkdir();
        Assert.assertTrue(TEMP_FOLDER.isDirectory());
    }

    @After
    public void tearDown() {
        final File[] allSubFiles = TEMP_FOLDER.listFiles();
        for (File subFile : allSubFiles) {
            Contract.requireTrue(subFile.delete());
        }
        Contract.requireTrue(TEMP_FOLDER.delete());
    }

    private IFileTracer createNewFileTracerMock() {
        IFileTracer fileTracerMock = mock(IFileTracer.class);
        setMockDefaults(fileTracerMock);
        return fileTracerMock;
    }

    private void setMockDefaults(IFileTracer mock) {
        final Answer openBehaviourAnswer = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final File traceFile = new File((String) invocation.getArguments()[0]);
                Assert.assertTrue(traceFile.isFile() || traceFile.createNewFile());

                when(mock.getFileSize()).thenReturn(traceFile.length());
                when(mock.getCurrentFilePath()).thenReturn(traceFile.getAbsolutePath());

                return null;
            }
        };

        doAnswer(openBehaviourAnswer).when(mock).openExistingFile(anyString());
        doAnswer(openBehaviourAnswer).when(mock).openNewFile(anyString());
    }

    @Test
    public void shouldTraceMessage() throws Exception {
        //region Initialization
        IFileTracer fileTracerMock = createNewFileTracerMock();

        ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath(), fileTracerMock, new ProductTracer.Settings(), null);
        //endregion

        //region Test
        productTracer.traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);
        productTracer.close();

        verify(fileTracerMock, times(1)).openNewFile(contains(TEMP_FOLDER.getAbsolutePath()));
        verify(fileTracerMock, times(1)).traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);
        //endregion
    }

    @Test
    public void shouldOpenLastProcessingFile() throws Exception {
        //region Test
        ArgumentCaptor<String> traceFileArgument = ArgumentCaptor.forClass(String.class);

        // stage 1: Start tracer, write new message & stop
        {
            IFileTracer fileTracerMock = createNewFileTracerMock();

            ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath(), fileTracerMock, new ProductTracer.Settings(), null);
            verify(fileTracerMock).openNewFile(traceFileArgument.capture());
            Assert.assertTrue(traceFileArgument.getValue().contains(TEMP_FOLDER.getAbsolutePath()));

            productTracer.traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);
            verify(fileTracerMock).traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);

            productTracer.pause();
            productTracer.close();
        }

        // stage 2: The same file should be picked
        {
            IFileTracer fileTracerMock = createNewFileTracerMock();

            final String newTraceMessage = DUMMY_TRACE_MESSAGE + 1;
            final TraceLevel newTraceLevel = TraceLevel.Always;

            ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath()
                    , fileTracerMock
                    , new ProductTracer.Settings() // also check that by default is true
                    , null);
            productTracer.traceMessage(newTraceLevel, newTraceMessage);

            verify(fileTracerMock).openExistingFile(traceFileArgument.getValue());
            verify(fileTracerMock).traceMessage(newTraceLevel, newTraceMessage);
        }
        //endregion
    }

    @Test
    public void shouldFinishLastProcessingFileAndStartNew() throws Exception {
        //region Test

        // stage 1: Start tracer, write new message & stop
        {
            IFileTracer fileTracerMock = createNewFileTracerMock();
            ArgumentCaptor<String> traceFileArgument = ArgumentCaptor.forClass(String.class);

            ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath(), fileTracerMock, new ProductTracer.Settings(), null);
            verify(fileTracerMock).openNewFile(traceFileArgument.capture());
            Assert.assertTrue(traceFileArgument.getValue().contains(TEMP_FOLDER.getAbsolutePath()));

            productTracer.traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);
            verify(fileTracerMock).traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);

            productTracer.finish();
            productTracer.close();
        }

        // stage 2: The same file should be picked
        {
            IFileTracer fileTracerMock = createNewFileTracerMock();
            ArgumentCaptor<String> traceFileArgument = ArgumentCaptor.forClass(String.class);

            final String newTraceMessage = DUMMY_TRACE_MESSAGE + 1;
            final TraceLevel newTraceLevel = TraceLevel.Always;

            ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath()
                    , fileTracerMock
                    , new ProductTracer.Settings() // also check that by default is true
                    , null);
            verify(fileTracerMock).openNewFile(traceFileArgument.capture());

            productTracer.traceMessage(newTraceLevel, newTraceMessage);
            verify(fileTracerMock).traceMessage(newTraceLevel, newTraceMessage);

            productTracer.close();
        }
        //endregion
    }

    @Test
    public void shouldDoNothingIfUsedAfterShutdown() throws Exception {
        //region Initialization
        IFileTracer fileTracerMock = createNewFileTracerMock();

        ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath()
                , fileTracerMock
                , new ProductTracer.Settings()
                , null);
        //endregion

        //region Test
        productTracer.close();
        productTracer.finish();
        productTracer.pause();
        productTracer.close();
        productTracer.traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);

        verify(fileTracerMock, times(0)).traceMessage(anyObject(), anyString());
        //endregion
    }

    @Test
    public void shouldReturnAllTraceFiles() throws Exception {
        final int TriesCount = 10;

        //region Test
        for (int fileIndex = 0; fileIndex < TriesCount; ++fileIndex) {
            ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath()
                    , createNewFileTracerMock()
                    , new ProductTracer.Settings(false, ProductTracer.Settings.DEFAULT_SIZE)
                    , null);
            productTracer.finish();
        }

        // create additional non trace files
        Assert.assertTrue(new File(TEMP_FOLDER, "some_other_file").createNewFile());

        Assert.assertEquals(TEMP_FOLDER.listFiles().length - 1, ProductTracer.getAllTraceFiles(TEMP_FOLDER.getAbsolutePath()).length);
        //endregion
    }

    @Test
    public void shouldRemoveOldFilesIfDiskQuotaWasExceededOnStart() throws Exception{
        final long TraceFilesDiskQuota = 10;

        String initialFilePath;

        // Step 1: Create non empty trace file and overfill it
        {
            IFileTracer fileTracerMock = createNewFileTracerMock();

            ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath()
                    , fileTracerMock
                    , new ProductTracer.Settings()
                    , null);
            productTracer.pause();
            initialFilePath = fileTracerMock.getCurrentFilePath();

            FileWriter fileWriter = new FileWriter(fileTracerMock.getCurrentFilePath());
            char[] content = new char[(int) TraceFilesDiskQuota];
            Arrays.fill(content, '1');
            fileWriter.write(content);
            fileWriter.close();
        }

        // Step 2: Check that the new file is created and the old one is deleted
        {
            IFileTracer fileTracerMock = createNewFileTracerMock();
            IProductTracerEvents productTracerEventsMock = mock(IProductTracerEvents.class);

            ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath()
                    , fileTracerMock
                    , new ProductTracer.Settings(false, TraceFilesDiskQuota)
                    , productTracerEventsMock);
            productTracer.finish();

            ArgumentCaptor<String> newFilePathCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileTracerMock).openNewFile(newFilePathCaptor.capture());
            Assert.assertNotEquals(initialFilePath, newFilePathCaptor);

            ArgumentCaptor<File[]> removedFilesCaptor = ArgumentCaptor.forClass(File[].class);
            verify(productTracerEventsMock).onRemoveFiles(removedFilesCaptor.capture());

            final File[] actualRemovedFiles = removedFilesCaptor.getValue();
            Assert.assertEquals(1, actualRemovedFiles.length);
            Assert.assertEquals(initialFilePath, actualRemovedFiles[0].getAbsolutePath());
            Assert.assertFalse(actualRemovedFiles[0].exists());
        }
    }

    @Test
    public void shouldRemoveOldFilesIfDiskQuotaWasExceeded() {
        final long TraceFilesDiskQuota = 40;

        //region Initialization
        IFileTracer fileTracerMock = createNewFileTracerMock();
        ProductTracer productTracer = new ProductTracer(TEMP_FOLDER.getAbsolutePath()
                , fileTracerMock
                , new ProductTracer.Settings(true, TraceFilesDiskQuota)
                , null);
        verify(fileTracerMock, times(1)).openNewFile(anyString());
        //endregion

        //region Test
        final int DummyNearOverflowIterationsNumber = 5;
        for (int iterationIndex = 0; iterationIndex < DummyNearOverflowIterationsNumber; ++iterationIndex) {
            when(fileTracerMock.getFileSize())
                    .thenReturn((TraceFilesDiskQuota / DummyNearOverflowIterationsNumber)*iterationIndex);
            productTracer.traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);
        }
        verify(fileTracerMock, times(1)).openNewFile(anyString());

        when(fileTracerMock.getFileSize()).thenReturn(TraceFilesDiskQuota + 1);
        productTracer.traceMessage(DUMMY_TRACE_LEVEL, DUMMY_TRACE_MESSAGE);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileTracerMock, times(2)).openNewFile(argumentCaptor.capture());
        Assert.assertTrue(argumentCaptor.getAllValues().get(1).startsWith(TEMP_FOLDER.getAbsolutePath()));
        //endregion
    }
}
