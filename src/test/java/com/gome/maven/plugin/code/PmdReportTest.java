package com.gome.maven.plugin.code;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.gome.maven.plugin.code.check.PmdReport;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id: PmdReportTest.java 1793232 2017-04-29 19:15:09Z adangel $
 */
public class PmdReportTest
        extends AbstractPmdReportTest {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp()throws Exception {
        super.setUp();
        FileUtils.deleteDirectory(new File(getBasedir(), "target/test/unit"));
    }

    public void testDefaultConfiguration()
            throws Exception {
        FileUtils.copyDirectoryStructure(new File(getBasedir(),
                        "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/default-configuration/target/site"));

        File testPom =
                new File(getBasedir(),
                        "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // check if the rulesets, that have been applied, have been copied
        generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/java-basic.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/java-imports.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/java-unusedcode.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html");

        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // check if there's a link to the JXR files
        String str = readFile(new File(getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html"));

        assertTrue(str.contains("/xref/def/configuration/App.html#L31"));

        assertTrue(str.contains("/xref/def/configuration/AppSample.html#L45"));
    }

    public void testDefaultConfigurationWithAnalysisCache()
            throws Exception {
        FileUtils.copyDirectoryStructure(new File(getBasedir(),
                        "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/pmd-with-analysis-cache-plugin-config/target/site"));

        File testPom =
                new File(getBasedir(),
                        "src/test/resources/unit/default-configuration/pmd-with-analysis-cache-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
        mojo.execute();

        // check if the PMD analysis cache file has been generated
        File cacheFile = new File(getBasedir(), "target/test/unit/pmd-with-analysis-cache-plugin-config/target/pmd/pmd.cache");
        assertTrue(FileUtils.fileExists(cacheFile.getAbsolutePath()));
    }


    /**
     * Verify skip parameter
     *
     * @throws Exception
     */
    public void testSkipConfiguration()
            throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/unit/custom-configuration/skip-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
        mojo.execute();

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File(getBasedir(), "target/test/unit/skip-configuration/target/pmd.csv");
        assertFalse(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/custom.xml");
        assertFalse(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html");
        assertFalse(FileUtils.fileExists(generatedFile.getAbsolutePath()));
    }

    public void testSkipEmptyReportConfiguration()
            throws Exception {
        File testPom =
                new File(getBasedir(), "src/test/resources/unit/empty-report/skip-empty-report-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
        mojo.execute();

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File(getBasedir(), "target/test/unit/empty-report/target/site/pmd.html");
        assertFalse(FileUtils.fileExists(generatedFile.getAbsolutePath()));
    }

    public void testEmptyReportConfiguration()
            throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/unit/empty-report/empty-report-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
        mojo.execute();

        // verify the generated files do exist, even if there are no violations
        File generatedFile = new File(getBasedir(), "target/test/unit/empty-report/target/site/pmd.html");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));
        String str = readFile(new File(getBasedir(), "target/test/unit/empty-report/target/site/pmd.html"));

    }


    public void testInvalidTargetJdk()
            throws Exception {
        try {
            File testPom =
                    new File(getBasedir(), "src/test/resources/unit/invalid-format/invalid-target-jdk-plugin-config.xml");
            PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
            mojo.execute();

            fail("Must throw MavenReportException.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Read the contents of the specified file object into a string
     *
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException
     */
    private String readFile(File file)
            throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final StringBuilder str = new StringBuilder((int) file.length());

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                str.append(' ');
                str.append(line);
            }
            return str.toString();
        }
    }


    /**
     * Verify that suppressMarker works
     *
     * @throws Exception
     */
    public void testSuppressMarkerConfiguration()
            throws Exception {
        File testPom =
                new File(getBasedir(),
                        "src/test/resources/unit/default-configuration/pmd-with-suppressMarker-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        String str = readFile(new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml"));

        // check that there is no violation reported for "unusedVar2" - as it is suppressed
        assertFalse(str.contains("Avoid unused private fields such as 'unusedVar2'."));
    }


    public void testPMDProcessingError()
            throws Exception {
        File testPom = new File(getBasedir(),
                "src/test/resources/unit/processing-error/pmd-processing-error-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("pmd", testPom);
        try {
            mojo.execute();
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().endsWith("Found 1 PMD processing errors"));
        }
    }

    public void testPMDExcludeRootsShouldExcludeSubdirectories() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/unit/exclude-roots/pmd-exclude-roots-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo("check", testPom);
        mojo.execute();

        File generatedFile = new File(getBasedir(), "target/test/unit/exclude-roots/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));
        String str = readFile(generatedFile);

        assertTrue("Seems like all directories are excluded now", str.contains("ForLoopShouldBeWhileLoop"));
        assertFalse("Exclusion of an exact source directory not working", str.contains("OverrideBothEqualsAndHashcode"));
        assertFalse("Exclusion of basedirectory with subdirectories not working (MPMD-178)", str.contains("JumbledIncrementer"));
    }

    public void testViolation()throws Exception {
        File testPom =
                new File(getBasedir(),
                        "src/test/resources/unit/default-configuration/pmd-report-my-test.xml");
        final PmdReport mojo = (PmdReport) lookupMojo("check", testPom);
        mojo.execute();

//        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
//        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));
//        String str = readFile(generatedFile);
//
//        assertEquals(0, StringUtils.countMatches(str, "<violation"));
    }
}
