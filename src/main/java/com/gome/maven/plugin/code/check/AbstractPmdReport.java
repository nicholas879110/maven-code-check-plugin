package com.gome.maven.plugin.code.check;

import net.sourceforge.pmd.PMD;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class AbstractPmdReport extends AbstractMavenMojo {

    /**
     * Location of the file.
     * //@parameter expression="${project}"
     * //@readonly
     */
   @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * A list of files to exclude from checking. Can contain Ant-style wildcards and double wildcards. Note that these
     * exclusion patterns only operate on the path of a source file relative to its source root directory. In other
     * words, files are excluded based on their package and/or class name. If you want to exclude entire source root
     * directories, use the parameter <code>excludeRoots</code> instead.
     *
     * @since 2.2
     */
    @Parameter
    private List<String> excludes;

    /**
     * A list of files to include from checking. Can contain Ant-style wildcards and double wildcards. Defaults to
     * **\/*.java.
     *
     * @since 2.2
     */
    @Parameter
    private List<String> includes;

    /**
     * Specifies the location of the source directories to be used for PMD.
     * Defaults to <code>project.compileSourceRoots</code>.
     *
     * @since 3.7
     */
    @Parameter(defaultValue = "${project.compileSourceRoots}")
    private List<String> compileSourceRoots;

    /**
     * The directories containing the test-sources to be used for PMD.
     * Defaults to <code>project.testCompileSourceRoots</code>
     *
     * @since 3.7
     */
    @Parameter(defaultValue = "${project.testCompileSourceRoots}")
    private List<String> testSourceRoots;

    /**
     * The project source directories that should be excluded.
     *
     * @since 2.2
     */
    @Parameter
    private File[] excludeRoots;

    /**
     * Run PMD on the tests.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = "false")
    protected boolean includeTests;


    /**
     * The file encoding to use when reading the Java sources.
     *
     * @since 2.3
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    private String sourceEncoding;


    /**
     * File that lists classes and rules to be excluded from failures.
     * For PMD, this is a properties file. For CPD, this
     * is a text file that contains comma-separated lists of classes
     * that are allowed to duplicate.
     *
     * @since 3.7
     */
    @Parameter(property = "pmd.excludeFromFailureFile", defaultValue = "")
    protected String excludeFromFailureFile;


    /**
     * The files that are being analyzed.
     */
    protected Map<File, PmdFileInfo> filesToProcess;

    /**
     * {@inheritDoc}
     */
    @Override
    protected MavenProject getProject() {
        return project;
    }


    /**
     * Convenience method to get the list of files where the PMD tool will be executed
     *
     * @return a List of the files where the PMD tool will be executed
     * @throws java.io.IOException
     */
    protected Map<File, PmdFileInfo> getFilesToProcess()throws IOException {
        if (excludeRoots == null) {
            excludeRoots = new File[0];
        }

        Collection<File> excludeRootFiles = new HashSet<File>(excludeRoots.length);

        for (File file : excludeRoots) {
            if (file.isDirectory()) {
                excludeRootFiles.add(file);
            }
        }

        List<PmdFileInfo> directories = new ArrayList<PmdFileInfo>();
        if (null == compileSourceRoots) {
            compileSourceRoots = project.getCompileSourceRoots();
        }
        if (compileSourceRoots != null) {
            for (String root : compileSourceRoots) {
                File sroot = new File(root);
                if (sroot.exists()) {
                    directories.add(new PmdFileInfo(project, sroot, null));
                }
            }
        }

        if (null == testSourceRoots) {
            testSourceRoots = project.getTestCompileSourceRoots();
        }
        if (includeTests) {
            if (testSourceRoots != null) {
                for (String root : testSourceRoots) {
                    File sroot = new File(root);
                    if (sroot.exists()) {
//                        String testXref = constructXRefLocation(true);
                        directories.add(new PmdFileInfo(project, sroot, null));
                    }
                }
            }
        }
//        if (aggregate) {
//            for (MavenProject localProject : reactorProjects) {
//                @SuppressWarnings("unchecked")
//                List<String> localCompileSourceRoots = localProject.getCompileSourceRoots();
//                for (String root : localCompileSourceRoots) {
//                    File sroot = new File(root);
//                    if (sroot.exists()) {
//                        String sourceXref = constructXRefLocation(false);
//                        directories.add(new PmdFileInfo(localProject, sroot, sourceXref));
//                    }
//                }
//                if (includeTests) {
//                    @SuppressWarnings("unchecked")
//                    List<String> localTestCompileSourceRoots = localProject.getTestCompileSourceRoots();
//                    for (String root : localTestCompileSourceRoots) {
//                        File sroot = new File(root);
//                        if (sroot.exists()) {
//                            String testXref = constructXRefLocation(true);
//                            directories.add(new PmdFileInfo(localProject, sroot, testXref));
//                        }
//                    }
//                }
//            }
//
//        }

        String excluding = getExcludes();
        getLog().debug("Exclusions: " + excluding);
        String including = getIncludes();
        getLog().debug("Inclusions: " + including);

        Map<File, PmdFileInfo> files = new TreeMap<File, PmdFileInfo>();

        for (PmdFileInfo finfo : directories) {
            getLog().debug("Searching for files in directory " + finfo.getSourceDirectory().toString());
            File sourceDirectory = finfo.getSourceDirectory();
            if (sourceDirectory.isDirectory() && !isDirectoryExcluded(excludeRootFiles, sourceDirectory)) {
                List<File> newfiles = FileUtils.getFiles(sourceDirectory, including, excluding);
                for (File newfile : newfiles) {
                    files.put(newfile.getCanonicalFile(), finfo);
                }
            }
        }

        return files;
    }

    private boolean isDirectoryExcluded(Collection<File> excludeRootFiles, File sourceDirectoryToCheck) {
        boolean returnVal = false;
        for (File excludeDir : excludeRootFiles) {
            try {
                if (sourceDirectoryToCheck.getCanonicalPath().startsWith(excludeDir.getCanonicalPath())) {
                    getLog().debug("Directory " + sourceDirectoryToCheck.getAbsolutePath()
                            + " has been excluded as it matches excludeRoot "
                            + excludeDir.getAbsolutePath());
                    returnVal = true;
                    break;
                }
            } catch (IOException e) {
                getLog().warn("Error while checking " + sourceDirectoryToCheck
                        + " whether it should be excluded.", e);
            }
        }
        return returnVal;
    }

    /**
     * Gets the comma separated list of effective include patterns.
     *
     * @return The comma separated list of effective include patterns, never <code>null</code>.
     */
    private String getIncludes() {
        Collection<String> patterns = new LinkedHashSet<String>();
        if (includes != null) {
            patterns.addAll(includes);
        }
        if (patterns.isEmpty()) {
            patterns.add("**/*.java");
        }
        return StringUtils.join(patterns.iterator(), ",");
    }

    /**
     * Gets the comma separated list of effective exclude patterns.
     *
     * @return The comma separated list of effective exclude patterns, never <code>null</code>.
     */
    private String getExcludes() {
        Collection<String> patterns = new LinkedHashSet<String>(FileUtils.getDefaultExcludesAsList());
        if (excludes != null) {
            patterns.addAll(excludes);
        }
        return StringUtils.join(patterns.iterator(), ",");
    }



    protected String getSourceEncoding() {
        return sourceEncoding;
    }

    static String getPmdVersion() {
        try {
            return (String) PMD.class.getField("VERSION").get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("PMD VERSION field not accessible", e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("PMD VERSION field not found", e);
        }
    }

}
