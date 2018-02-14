package com.gome.maven.plugin.code.check;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.gome.maven.log.CodeCheckSystemStreamLog;
import com.gome.maven.log.LogInitializer;
import com.gome.maven.idea.Main;
import com.gome.maven.plugin.code.pmd.inspection.AliLocalInspectionToolProvider;
import com.gome.maven.plugin.code.pmd.inspection.LocalInspectionTool;
import com.gome.maven.plugin.code.pmd.inspection.ProblemDescriptor;
import com.gome.maven.plugin.code.pmd.inspection.RuleInfo;
import com.gome.maven.plugin.code.pmd.util.HighlightDisplayLevel;
import com.gome.maven.plugin.code.pmd.util.HighlightDisplayLevels;
import com.gome.maven.util.ReflectionUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * //@goal check
 * //@phase compile
 * //@requiresProject false
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.COMPILE, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class PmdReport extends AbstractPmdReport {

    private static final CodeCheckSystemStreamLog LOG=LogInitializer.getInstance().getLog();

    /**
     * The target JDK to analyze based on. Should match the target used in the compiler plugin. Valid values are
     * currently <code>1.3</code>, <code>1.4</code>, <code>1.5</code>, <code>1.6</code>, <code>1.7</code> and
     * <code>1.8</code>.
     * <p>
     * <b>Note:</b> this parameter is only used if the language parameter is set to <code>java</code>.
     * </p>
     */
    @Parameter(property = "targetJdk", defaultValue = "${maven.compiler.target}")
    private String targetJdk;

    /**
     * The programming language to be analyzed by PMD. Valid values are currently <code>java</code>,
     * <code>javascript</code> and <code>jsp</code>.
     *
     * @since 3.0
     */
    @Parameter(defaultValue = "java")
    private String language;

    /**
     * The rule priority threshold; rules with lower priority than this will not be evaluated.
     *
     * @since 2.1
     */
    @Parameter(property = "minimumPriority", defaultValue = "5")
    private int minimumPriority = 5;

    /**
     * Skip the PMD report generation. Most useful on the command line via "-Dpmd.skip=true".
     *
     * @since 2.1
     */
    @Parameter(property = "pmd.skip", defaultValue = "false")
    private boolean skip;

    /**
     * The PMD rulesets to use. See the
     * <a href="http://pmd.github.io/pmd-5.5.1/pmd-java/rules/index.html">Stock Java Rulesets</a> for a
     * list of some included. Defaults to the java-basic, java-empty, java-imports, java-unnecessary
     * and java-unusedcode rulesets.
     */
    @Parameter
    private String[] rulesets = new String[]{};

    /**
     * Controls whether the project's compile/test classpath should be passed to PMD to enable its type resolution
     * feature.
     *
     * @since 3.0
     */
    @Parameter(property = "pmd.typeResolution", defaultValue = "true")
    private boolean typeResolution;

    /**
     * Controls whether PMD will track benchmark information.
     *
     * @since 3.1
     */
    @Parameter(property = "pmd.benchmark", defaultValue = "false")
    private boolean benchmark;

    /**
     * Benchmark output filename.
     *
     * @since 3.1
     */
    @Parameter(property = "pmd.benchmarkOutputFilename",
            defaultValue = "${project.build.directory}/pmd-benchmark.txt")
    private String benchmarkOutputFilename;

    /**
     * Source level marker used to indicate whether a RuleViolation should be suppressed. If it is not set, PMD's
     * default will be used, which is <code>NOPMD</code>. See also <a
     * href="https://pmd.github.io/latest/usage/suppressing.html">PMD &#x2013; Suppressing warnings</a>.
     *
     * @since 3.4
     */
    @Parameter(property = "pmd.suppressMarker")
    private String suppressMarker;

    /**
     * per default pmd executions error are ignored to not break the whole
     *
     * @since 3.1
     */
    @Parameter(property = "pmd.skipPmdError", defaultValue = "true")
    private boolean skipPmdError;

    /**
     * Enables the analysis cache, which speeds up PMD. This
     * requires a cache file, that contains the results of the last
     * PMD run. Thus the cache is only effective, if this file is
     * not cleaned between runs.
     *
     * @since 3.8
     */
    @Parameter(property = "pmd.analysisCache", defaultValue = "false")
    private boolean analysisCache;

    @Parameter
    private String[] skipRulesets = new String[]{};


//
//    private void executePmdWithClassloader()
//            throws MavenReportException {
//        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
//        try {
//            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
//            executePmd();
//        } finally {
//            Thread.currentThread().setContextClassLoader(origLoader);
//        }
//    }

//    private void executePmd()
//            throws MavenReportException {
//        if (renderer != null) {
//            // PMD has already been run
//            getLog().debug("PMD has already been run - skipping redundant execution.");
//            return;
//        }
//
//        try {
//            excludeFromFile.loadExcludeFromFailuresData(excludeFromFailureFile);
//        } catch (MojoExecutionException e) {
//            throw new MavenReportException("Unable to load exclusions", e);
//        }
//
//        // configure ResourceManager
//        locator.addSearchPath(FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath());
//        locator.addSearchPath("url", "");
//        locator.setOutputDirectory(targetDirectory);
//
//        renderer = new PmdCollectingRenderer();
//        PMDConfiguration pmdConfiguration = getPMDConfiguration();
//
//        String[] sets = new String[rulesets.length];
//        try {
//            for (int idx = 0; idx < rulesets.length; idx++) {
//                String set = rulesets[idx];
//                getLog().debug("Preparing ruleset: " + set);
//                RuleSetReferenceId id = new RuleSetReferenceId(set);
//                File ruleset = locator.getResourceAsFile(id.getRuleSetFileName(), getLocationTemp(set));
//                if (null == ruleset) {
//                    throw new MavenReportException("Could not resolve " + set);
//                }
//                sets[idx] = ruleset.getAbsolutePath();
//            }
//        } catch (ResourceNotFoundException e) {
//            throw new MavenReportException(e.getMessage(), e);
//        } catch (FileResourceCreationException e) {
//            throw new MavenReportException(e.getMessage(), e);
//        }
//        pmdConfiguration.setRuleSets(StringUtils.join(sets, ","));
//
//        try {
//            if (filesToProcess == null) {
//                filesToProcess = getFilesToProcess();
//            }
//
//            if (filesToProcess.isEmpty() && !"java".equals(language)) {
//                getLog().warn("No files found to process. Did you add your additional source folders like javascript?"
//                        + " (see also build-helper-maven-plugin)");
//            }
//        } catch (IOException e) {
//            throw new MavenReportException("Can't get file list", e);
//        }
//
//        String encoding = getSourceEncoding();
//        if (StringUtils.isEmpty(encoding) && !filesToProcess.isEmpty()) {
//            getLog().warn("File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
//                    + ", i.e. build is platform dependent!");
//            encoding = ReaderFactory.FILE_ENCODING;
//        }
//        pmdConfiguration.setSourceEncoding(encoding);
//
//        List<DataSource> dataSources = new ArrayList<>(filesToProcess.size());
//        for (File f : filesToProcess.keySet()) {
//            dataSources.add(new FileDataSource(f));
//        }
//
//        if (sets.length > 0) {
//            processFilesWithPMD(pmdConfiguration, dataSources);
//        } else {
//            getLog().debug("Skipping PMD execution as no rulesets are defined.");
//        }
//
//        if (renderer.hasErrors()) {
//            if (!skipPmdError) {
//                getLog().error("PMD processing errors:");
//                getLog().error(renderer.getErrorsAsString());
//                throw new MavenReportException("Found " + renderer.getErrors().size() + " PMD processing errors");
//            }
//            getLog().warn("There are " + renderer.getErrors().size() + " PMD processing errors:");
//            getLog().warn(renderer.getErrorsAsString());
//        }
//
//        removeExcludedViolations(renderer.getViolations());
//
//        // if format is XML, we need to output it even if the file list is empty or we have no violations
//        // so the "check" goals can check for violations
//        if (isXml() && renderer != null) {
//            writeNonHtml(renderer.asReport());
//        }
//
//        if (benchmark) {
//            try (PrintStream benchmarkFileStream = new PrintStream(benchmarkOutputFilename)) {
//                (new TextReport()).generate(Benchmarker.values(), benchmarkFileStream);
//            } catch (FileNotFoundException fnfe) {
//                getLog().error("Unable to generate benchmark file: " + benchmarkOutputFilename, fnfe);
//            }
//        }
//    }

//    private void removeExcludedViolations(List<RuleViolation> violations) {
//        getLog().debug("Removing excluded violations. Using " + excludeFromFile.countExclusions()
//                + " configured exclusions.");
//        int violationsBefore = violations.size();
//
//        Iterator<RuleViolation> iterator = violations.iterator();
//        while (iterator.hasNext()) {
//            RuleViolation rv = iterator.next();
//            if (excludeFromFile.isExcludedFromFailure(rv)) {
//                iterator.remove();
//            }
//        }
//
//        int numberOfExcludedViolations = violationsBefore - violations.size();
//        getLog().debug("Excluded " + numberOfExcludedViolations + " violations.");
//    }

//    private void processFilesWithPMD(PMDConfiguration pmdConfiguration, List<DataSource> dataSources)
//            throws MavenReportException {
//        RuleSetFactory ruleSetFactory = new RuleSetFactory(RuleSetFactory.class.getClassLoader(),
//                RulePriority.valueOf(this.minimumPriority), false, true);
//        RuleContext ruleContext = new RuleContext();
//
//        try {
//            getLog().debug("Executing PMD...");
//            PMD.processFiles(pmdConfiguration, ruleSetFactory, dataSources, ruleContext,
//                    Arrays.<Renderer>asList(renderer));
//
//            if (getLog().isDebugEnabled()) {
//                getLog().debug("PMD finished. Found " + renderer.getViolations().size() + " violations.");
//            }
//        } catch (Exception e) {
//            String message = "Failure executing PMD: " + e.getLocalizedMessage();
//            if (!skipPmdError) {
//                throw new MavenReportException(message, e);
//            }
//            getLog().warn(message, e);
//        }
//    }


    /**
     * Convenience method to get the location of the specified file name.
     *
     * @param name the name of the file whose location is to be resolved
     * @return a String that contains the absolute file name of the file
     */
    protected String getLocationTemp(String name) {
        String loc = name;
        if (loc.indexOf('/') != -1) {
            loc = loc.substring(loc.lastIndexOf('/') + 1);
        }
        if (loc.indexOf('\\') != -1) {
            loc = loc.substring(loc.lastIndexOf('\\') + 1);
        }

        // MPMD-127 in the case that the rules are defined externally on a url
        // we need to replace some special url characters that cannot be
        // used in filenames on disk or produce ackward filenames.
        // replace all occurrences of the following characters: ? : & = %
        loc = loc.replaceAll("[\\?\\:\\&\\=\\%]", "_");

        if (!loc.endsWith(".xml")) {
            loc = loc + ".xml";
        }

        getLog().debug("Before: " + name + " After: " + loc);
        return loc;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Dependency> dependencies=project.getDependencies();
        List<Artifact> artifacts=project.getCompileArtifacts();
//        project.getSystemClasspathElements()
        for (Dependency dependency:dependencies){
            System.out.println(dependency.toString());
            System.out.println(dependency.getSystemPath());
            System.out.println();
        }
        for (Artifact artifact:artifacts){
            System.out.println(artifact.toString());
            System.out.println(artifact.getFile());
            System.out.println();
        }


        if (skip == true) {
            getLog().info("skip code check!");
            return;
        }
        if (filesToProcess == null) {
            try {
                filesToProcess = getFilesToProcess();
            } catch (IOException e) {
                getLog().error("get filesToProcess to excute encounting error!");
            }
        }
        if (filesToProcess == null || filesToProcess.size() == 0) {
            getLog().info("no filesToProcess to excute!");
            return;
        }
        List<Class<?>> tools = AliLocalInspectionToolProvider.getInspectionClasses();
        List<Class<?>> localTools = null;
        if (rulesets != null && rulesets.length > 0) {
            localTools = new ArrayList<>();
            for (String ruleset : rulesets) {
                Class<?> localTool = getLocalTool(ruleset, tools);
                if (localTool == null) {
                    getLog().warn("ruleset:<" + ruleset + ">can not be found,please check your plugin config!");
                } else {
                    localTools.add(localTool);
                }
            }
        } else {
            localTools = new ArrayList<>(tools);
        }
        if (skipRulesets != null && skipRulesets.length > 0) {
            if (localTools.size() > 0) {
                for (String skipRuleset : skipRulesets) {
                    Class<?> localTool = getLocalTool(skipRuleset, tools);
                    if (localTool == null) {
                        getLog().warn("ruleset :<" + skipRuleset + ">can not be found,please check your plugin config!");
                    } else {
                        localTools.remove(localTool);
                    }
                }
            }
        }
        if (localTools.size() <= 0) {
            getLog().warn("no rulesets can not be found,please check your plugin config!");
            return;
        }
        if (minimumPriority > 5 || minimumPriority < 1) {
            getLog().warn("minimumPriority is between 1 and 5,please check your plugin config!");
            return;
        }
        for (final Class aClass : localTools) {
            LocalInspectionTool tool = (LocalInspectionTool) instantiateTool(aClass);
            RuleInfo ruleInfo = AliLocalInspectionToolProvider.getRuleInfoMap().get(tool.ruleName());
            if (ruleInfo.getRule().getPriority().getPriority() >= minimumPriority) {
                continue;
            }
            Set<Map.Entry<File, PmdFileInfo>> entries = filesToProcess.entrySet();
            for (Map.Entry<File, PmdFileInfo> entry : entries) {
                ProblemDescriptor[] problemDescriptors = tool.checkFile(entry.getKey(), false);
                if (problemDescriptors != null && problemDescriptors.length > 0) {
                    for (ProblemDescriptor problemDescriptor : problemDescriptors) {
                        HighlightDisplayLevel level = HighlightDisplayLevels.getHighlightDisplayLevel(problemDescriptor.getRule().getPriority());
                        if (level.equals(HighlightDisplayLevels.CRITICAL)) {
                            LOG.critical(problemDescriptor.toString());
                        } else if (level.equals(HighlightDisplayLevels.BLOCKER)) {
                            LOG.blocker(problemDescriptor.toString());
                        } else if (level.equals(HighlightDisplayLevels.MAJOR)) {
                            LOG.major(problemDescriptor.toString());
                        } else {
                            LOG.error(problemDescriptor.toString());
                        }
//                        throw new MojoExecutionException("code check failed,please fix your code first!");
                    }
                }
            }

        }
    }


    static Class<?> getLocalTool(String ruleName, List<Class<?>> toolClasss) {
        for (Class<?> toolClass : toolClasss) {
            if (toolClass.getName().equals(ruleName)) {
                return toolClass;
            }
        }
        return null;
    }


    static Object instantiateTool(Class<?> toolClass) {
        try {
            return ReflectionUtil.newInstance(toolClass, new Class[0]);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return null;
    }


//    /**
//     * Constructs the PMD configuration class, passing it an argument that configures the target JDK.
//     *
//     * @return the resulting PMD
//     * @throws org.apache.maven.reporting.MavenReportException if targetJdk is not supported
//     */
//    public PMDConfiguration getPMDConfiguration()
//            throws MavenReportException {
//        PMDConfiguration configuration = new PMDConfiguration();
//        LanguageVersion languageVersion = null;
//
//        if (("java".equals(language) || null == language) && null != targetJdk) {
//            languageVersion = LanguageRegistry.findLanguageVersionByTerseName("java " + targetJdk);
//            if (languageVersion == null) {
//                throw new MavenReportException("Unsupported targetJdk value '" + targetJdk + "'.");
//            }
//        } else if ("javascript".equals(language) || "ecmascript".equals(language)) {
//            languageVersion = LanguageRegistry.findLanguageVersionByTerseName("ecmascript");
//        } else if ("jsp".equals(language)) {
//            languageVersion = LanguageRegistry.findLanguageVersionByTerseName("jsp");
//        }
//
//        if (languageVersion != null) {
//            getLog().debug("Using language " + languageVersion);
//            configuration.setDefaultLanguageVersion(languageVersion);
//        }
//
//        if (typeResolution) {
//            try {
//                @SuppressWarnings("unchecked")
//                List<String> classpath =
//                        includeTests ? project.getTestClasspathElements() : project.getCompileClasspathElements();
//                getLog().debug("Using aux classpath: " + classpath);
//                configuration.prependClasspath(StringUtils.join(classpath.iterator(), File.pathSeparator));
//            } catch (Exception e) {
//                throw new MavenReportException(e.getMessage(), e);
//            }
//        }
//
//        if (null != suppressMarker) {
//            configuration.setSuppressMarker(suppressMarker);
//        }
//
//        configuration.setBenchmark(benchmark);
//
//        if (analysisCache) {
//            configuration.setAnalysisCacheLocation(analysisCacheLocation);
//            getLog().debug("Using analysis cache location: " + analysisCacheLocation);
//        }
//
//        return configuration;
//    }
//
//


}
