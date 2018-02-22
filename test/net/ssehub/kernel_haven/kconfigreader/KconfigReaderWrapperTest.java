package net.ssehub.kernel_haven.kconfigreader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.Util;

/**
 * Tests the KconfigReaderWrapper. These tests can only run on Linux, since
 * KconfigReader can only run on Linux.
 * 
 * @author Adam
 * @author Johannes
 * @author Manu
 */
@SuppressWarnings("null")
public class KconfigReaderWrapperTest {

    private static final File LINUX_DIR = new File("testdata/pseudoLinux/");

    private static final File RESOURCE_DIR = new File("testdata/tmp_res");

    private KconfigReaderWrapper wrapper;

    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void beforeClass() {
        Logger.init();
    }

    /**
     * Creates a new wrapper object for each test and creates the resource dir.
     */
    @Before
    public void setUp() {
        RESOURCE_DIR.mkdir();
        wrapper = new KconfigReaderWrapper(RESOURCE_DIR, LINUX_DIR);
    }

    /**
     * Deletes the resource directory after each test.
     * 
     * @throws IOException If deleting fails. 
     */
    @After
    public void tearDown() throws IOException {
        Util.deleteFolder(RESOURCE_DIR);
    }

    /**
     * Tests if prepareLinux actually is called.
     * 
     * @throws IOException
     *             unwanted.
     */
    @Test
    public void testPrepareLinux() throws IOException {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));

        assertThat(wrapper.prepareLinux(), is(true));

        File logfile = new File(LINUX_DIR, "test.log");
        assertThat(logfile.isFile(), is(true));

        BufferedReader br = new BufferedReader(new FileReader(logfile));
        assertThat(br.readLine(), is("Make allyesconfig called"));
        assertThat(br.readLine(), is("Make prepare called"));
        assertThat(br.readLine(), nullValue());

        // cleanup
        br.close();
        logfile.delete();
    }

    /**
     * Tests if dumpconf.exe is created by compileDumpconf and if the file is
     * executable.
     * 
     * @throws IOException
     *             unwanted.
     */
    @Test
    public void testCompileDumpconf() throws IOException {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));

        File dumpconfExe = wrapper.compileDumpconf();

        assertThat(dumpconfExe.isFile(), is(true));
        assertThat(dumpconfExe.canExecute(), is(true));

        dumpconfExe.delete();
    }

    /**
     * Tests if dumpconf.exe is created by compileDumpconf and if the file is
     * executable.
     * 
     * @throws IOException
     *             unwanted.
     */
    @Test
    public void testRunKconfigReader() throws IOException {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));

        File dumpconfExe = new File("testdata/dumpconf");

        // prerequisites
        dumpconfExe.setExecutable(true);
        assertThat(dumpconfExe.canExecute(), is(true));

        File basepath = wrapper.runKconfigReader(dumpconfExe, "x86");
        assertThat(basepath, notNullValue());

        // features check
        Set<String> allowedSet = new HashSet<>();
        allowedSet.add("CONFIG_A");
        allowedSet.add("CONFIG_B");
        allowedSet.add("CONFIG_C");
        allowedSet.add("CONFIG_C_MODULE");

        File modules = new File(basepath.getAbsoluteFile() + ".features");
        assertThat(modules.isFile(), is(true));
        BufferedReader br = new BufferedReader(new FileReader(modules));

        String line = br.readLine();
        while (line != null) {
            assertThat(allowedSet.contains(line), is(true));
            line = br.readLine();
        }

        br.close();

        // dimacs check
        File dimacs = new File(basepath.getAbsoluteFile() + ".dimacs");
        assertThat(dimacs.isFile(), is(true));

        // TODO: check if dimacs is valid

        // clean up dimacs
        File[] files = modules.getParentFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(basepath.getName());
            }
        });

        for (File file : files) {
            file.delete();
        }
    }
}
