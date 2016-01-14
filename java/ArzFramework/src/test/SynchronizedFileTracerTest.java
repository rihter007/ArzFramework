import com.arz_x.common.helpers.Contract;
import com.arz_x.tracer.SynchronizedFileTracer;
import com.arz_x.tracer.TraceLevel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Rihter on 07.01.2016.
 * Unit tests for SynchronizedFileTracer
 */
public class SynchronizedFileTracerTest {

    private static final String DUMMY_TRACE_MESSAGE = "Dummy trace message";

    private static final File TEMPORARY_FOLDER = new File("temp");
    private static final File TEST_FILE = new File(TEMPORARY_FOLDER, "test.log");

    @Before
    public void setUp() throws Exception {
        TEMPORARY_FOLDER.mkdir();
        Assert.assertTrue(TEMPORARY_FOLDER.isDirectory());
    }

    @After
    public void tearDown() throws Exception {
        final File[] allSubFiles = TEMPORARY_FOLDER.listFiles();
        for (File subFile : allSubFiles) {
            Contract.requireTrue(subFile.delete());
        }
        Contract.requireTrue(TEMPORARY_FOLDER.delete());
    }

    @Test
    public void shouldCreateNewFileWhenOpens() throws Exception {
        //region Initialization
        FileOutputStream fos = new FileOutputStream(TEST_FILE);
        fos.write(0x12345);
        fos.close();

        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Always, false);
        fileTracer.close();
        //endregion

        //region Test
        Assert.assertTrue(TEST_FILE.isFile());
        Assert.assertEquals(0, TEST_FILE.length());
        //endregion
    }

    @Test
    public void shouldAppendExistingFile() throws Exception {
        final int writtenByte = 0xfe;

        //region Initialization
        FileOutputStream fs = new FileOutputStream(TEST_FILE);
        fs.write(writtenByte);
        fs.close();

        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Always, true);
        fileTracer.close();
        //endregion

        //region Test
        FileInputStream fis = new FileInputStream(TEST_FILE);
        Assert.assertEquals(writtenByte, fis.read());
        fis.close();
        //endregion
    }

    @Test
    public void shouldWriteMessage() throws Exception {
        //region Initialization
        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Verbose, false);
        //endregion

        //region Test
        fileTracer.traceMessage(TraceLevel.Always, DUMMY_TRACE_MESSAGE);
        fileTracer.close();

        List<String> allTraceFileLines = Files.readAllLines(Paths.get(TEST_FILE.getAbsolutePath()), Charset.forName("UTF-8"));
        Assert.assertEquals(1, allTraceFileLines.size());
        Assert.assertTrue(allTraceFileLines.get(0).contains(DUMMY_TRACE_MESSAGE));
        //endregion
    }

    @Test
    public void shouldNotWriteMessageWithLowerTraceLevel() throws Exception {
        //region Initialization
        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Always, false);
        //endregion

        //region Test
        fileTracer.traceMessage(TraceLevel.Important, DUMMY_TRACE_MESSAGE);

        Assert.assertEquals(0, Files.size(Paths.get(TEST_FILE.getAbsolutePath())));
        //endregion
        fileTracer.close();
    }

    @Test
    public void shouldReturnCorrectFileSize() throws Exception {
        //region Initialization
        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Verbose, false);
        //endregion

        //region Test
        fileTracer.traceMessage(TraceLevel.Always, DUMMY_TRACE_MESSAGE);
        Assert.assertEquals(Files.size(Paths.get(TEST_FILE.getAbsolutePath())), fileTracer.getFileSize());
        //endregion
        fileTracer.close();
    }

    @Test
    public void shouldReturnCorrectFilesSizeIfAppendsExistingFile() throws Exception {
        //region Initialization
        FileWriter fileWriter = new FileWriter(TEST_FILE);
        fileWriter.write("some long long long meaningless string");
        fileWriter.close();

        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Verbose, true);
        //endregion

        //region Test
        fileTracer.traceMessage(TraceLevel.Always, DUMMY_TRACE_MESSAGE);
        Assert.assertEquals(Files.size(Paths.get(TEST_FILE.getAbsolutePath())), fileTracer.getFileSize());
        //endregion
        fileTracer.close();
    }

    @Test
    public void shouldOpenExistingFile() throws Exception{
        final String initialFileString = "some_string";
        final File newTraceFile = new File(TEST_FILE.getAbsolutePath() + "1234");

        FileWriter fileWriter = new FileWriter(newTraceFile);
        fileWriter.write(initialFileString + System.getProperty("line.separator"));
        fileWriter.close();

        //region Initialization
        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Verbose, true);
        //endregion

        //region Test
        fileTracer.openExistingFile(newTraceFile.getAbsolutePath());
        fileTracer.traceMessage(TraceLevel.Always, DUMMY_TRACE_MESSAGE);
        Assert.assertEquals(newTraceFile.getAbsolutePath(), fileTracer.getCurrentFilePath());

        List<String> allFileLines = Files.readAllLines(Paths.get(newTraceFile.getAbsolutePath()));
        Assert.assertEquals(2, allFileLines.size());
        Assert.assertEquals(initialFileString, allFileLines.get(0));
        Assert.assertTrue(allFileLines.get(1).contains(DUMMY_TRACE_MESSAGE));

        Assert.assertEquals(Files.size(Paths.get(newTraceFile.getAbsolutePath())), fileTracer.getFileSize());
        //endregion

        fileTracer.close();
    }

    @Test
    public void shouldOpenNewFile() throws Exception{
        final String initialFileString = "some_string";
        final File newTraceFile = new File(TEST_FILE.getAbsolutePath() + "1234");

        FileWriter fileWriter = new FileWriter(newTraceFile);
        fileWriter.write(initialFileString + System.getProperty("line.separator"));
        fileWriter.close();

        //region Initialization
        SynchronizedFileTracer fileTracer = new SynchronizedFileTracer(TEST_FILE.getAbsolutePath()
                , TraceLevel.Verbose, true);
        //endregion

        //region Test
        fileTracer.openNewFile(newTraceFile.getAbsolutePath());
        fileTracer.traceMessage(TraceLevel.Always, DUMMY_TRACE_MESSAGE);
        Assert.assertEquals(newTraceFile.getAbsolutePath(), fileTracer.getCurrentFilePath());

        List<String> allFileLines = Files.readAllLines(Paths.get(newTraceFile.getAbsolutePath()));
        Assert.assertEquals(1, allFileLines.size());
        Assert.assertTrue(allFileLines.get(0).contains(DUMMY_TRACE_MESSAGE));

        Assert.assertEquals(Files.size(Paths.get(newTraceFile.getAbsolutePath())), fileTracer.getFileSize());
        //endregion

        fileTracer.close();
    }
}
