/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.openapi.projectRoots;

import com.gome.maven.execution.configurations.GeneralCommandLine;
import com.gome.maven.execution.configurations.ParametersList;
import com.gome.maven.execution.configurations.SimpleJavaParameters;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtilRt;
import com.gome.maven.openapi.vfs.JarFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.encoding.EncodingManager;
import com.gome.maven.util.PathUtil;
import com.gome.maven.util.lang.UrlClassLoader;
import gnu.trove.THashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author max
 */
public class JdkUtil {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.projectRoots.JdkUtil");
    private static final String WRAPPER_CLASS = "com.gome.maven.rt.execution.CommandLineWrapper";

    private JdkUtil() { }

    /**
     * @return the specified attribute of the JDK (examines rt.jar) or null if cannot determine the value
     */
    
    public static String getJdkMainAttribute( Sdk jdk, Attributes.Name attribute) {
        VirtualFile homeDirectory = jdk.getHomeDirectory();
        if (homeDirectory == null) return null;

        VirtualFile rtJar = homeDirectory.findFileByRelativePath("jre/lib/rt.jar");                 // JDK
        if (rtJar == null) rtJar = homeDirectory.findFileByRelativePath("lib/rt.jar");              // JRE
        if (rtJar == null) rtJar = homeDirectory.findFileByRelativePath("jre/lib/vm.jar");          // IBM JDK
        if (rtJar == null) rtJar = homeDirectory.findFileByRelativePath("../Classes/classes.jar");  // Apple JDK

        if (rtJar == null) {
            String versionString = jdk.getVersionString();
            if (versionString != null) {
                final int start = versionString.indexOf("\"");
                final int end = versionString.lastIndexOf("\"");
                versionString = start >= 0 && (end > start)? versionString.substring(start + 1, end) : null;
            }
            return versionString;
        }

        VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(rtJar);
        return jarRoot != null ? getJarMainAttribute(jarRoot, attribute) : null;
    }

    
    public static String getJarMainAttribute( VirtualFile jarRoot, Attributes.Name attribute) {
        VirtualFile manifestFile = jarRoot.findFileByRelativePath(JarFile.MANIFEST_NAME);
        if (manifestFile != null) {
            try {
                InputStream stream = manifestFile.getInputStream();
                try {
                    return new Manifest(stream).getMainAttributes().getValue(attribute);
                }
                finally {
                    stream.close();
                }
            }
            catch (IOException e) {
                LOG.debug(e);
            }
        }

        return null;
    }

    public static boolean checkForJdk( String homePath) {
        return checkForJdk(new File(FileUtil.toSystemDependentName(homePath)));
    }

    public static boolean checkForJdk( File homePath) {
        File binPath = new File(homePath, "bin");
        if (!binPath.exists()) return false;

        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept( File f) {
                if (f.isDirectory()) return false;
                String name = FileUtil.getNameWithoutExtension(f);
                return "javac".equals(name) || "javah".equals(name);
            }
        };
        File[] children = binPath.listFiles(fileFilter);

        return children != null && children.length >= 2 &&
                checkForRuntime(homePath.getAbsolutePath());
    }

    public static boolean checkForJre( String homePath) {
        return checkForJre(new File(FileUtil.toSystemDependentName(homePath)));
    }

    public static boolean checkForJre( File homePath) {
        File binPath = new File(homePath, "bin");
        if (!binPath.exists()) return false;

        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept( File f) {
                return !f.isDirectory() && "java".equals(FileUtil.getNameWithoutExtension(f));
            }
        };
        File[] children = binPath.listFiles(fileFilter);

        return children != null && children.length >= 1 &&
                checkForRuntime(homePath.getAbsolutePath());
    }

    public static boolean checkForRuntime( String homePath) {
        return new File(homePath, "jre/lib/rt.jar").exists() ||          // JDK
                new File(homePath, "lib/rt.jar").exists() ||              // JRE
                new File(homePath, "lib/modules").exists() ||             // Jigsaw JDK/JRE
                new File(homePath, "../Classes/classes.jar").exists() ||  // Apple JDK
                new File(homePath, "jre/lib/vm.jar").exists() ||          // IBM JDK
                new File(homePath, "classes").isDirectory();              // custom build
    }

    public static GeneralCommandLine setupJVMCommandLine(final String exePath,
                                                         final SimpleJavaParameters javaParameters,
                                                         final boolean forceDynamicClasspath) {
        final GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(exePath);

        final ParametersList vmParametersList = javaParameters.getVMParametersList();
        commandLine.getEnvironment().putAll(javaParameters.getEnv());
        commandLine.setPassParentEnvironment(javaParameters.isPassParentEnvs());

        final Class commandLineWrapper;
        if ((commandLineWrapper = getCommandLineWrapperClass()) != null) {
            if (forceDynamicClasspath) {
                File classpathFile = null;
                File vmParamsFile = null;
                if (!vmParametersList.hasParameter("-classpath") && !vmParametersList.hasParameter("-cp")) {
                    if (javaParameters.isDynamicVMOptions() && useDynamicVMOptions()) {
                        try {
                            vmParamsFile = FileUtil.createTempFile("vm_params", null);
                            final PrintWriter writer = new PrintWriter(vmParamsFile);
                            try {
                                for (String param : vmParametersList.getList()) {
                                    if (param.startsWith("-D")) {
                                        writer.println(param);
                                    }
                                }
                            }
                            finally {
                                writer.close();
                            }
                        }
                        catch (IOException e) {
                            LOG.error(e);
                        }
                        final List<String> list = vmParametersList.getList();
                        for (String param : list) {
                            if (!param.trim().startsWith("-D")) {
                                commandLine.addParameter(param);
                            }
                        }
                    }
                    else {
                        commandLine.addParameters(vmParametersList.getList());
                    }
                    try {
                        classpathFile = FileUtil.createTempFile("classpath", null);
                        final PrintWriter writer = new PrintWriter(classpathFile);
                        try {
                            for (String path : javaParameters.getClassPath().getPathList()) {
                                writer.println(path);
                            }
                        }
                        finally {
                            writer.close();
                        }

                        String classpath = PathUtil.getJarPathForClass(commandLineWrapper);
                        final String utilRtPath = PathUtil.getJarPathForClass(StringUtilRt.class);
                        if (!classpath.equals(utilRtPath)) {
                            classpath += File.pathSeparator + utilRtPath;
                        }
                        final Class<UrlClassLoader> ourUrlClassLoader = UrlClassLoader.class;
                        if (ourUrlClassLoader.getName().equals(vmParametersList.getPropertyValue("java.system.class.loader"))) {
                            classpath += File.pathSeparator + PathUtil.getJarPathForClass(ourUrlClassLoader);
                            classpath += File.pathSeparator + PathUtil.getJarPathForClass(THashMap.class);
                        }

                        commandLine.addParameter("-classpath");
                        commandLine.addParameter(classpath);
                    }
                    catch (IOException e) {
                        LOG.error(e);
                    }
                }

                appendEncoding(javaParameters, commandLine, vmParametersList);
                if (classpathFile != null) {
                    commandLine.addParameter(commandLineWrapper.getName());
                    commandLine.addParameter(classpathFile.getAbsolutePath());
                }

                if (vmParamsFile != null) {
                    commandLine.addParameter("@vm_params");
                    commandLine.addParameter(vmParamsFile.getAbsolutePath());
                }
            }
            else {
                appendParamsEncodingClasspath(javaParameters, commandLine, vmParametersList);
            }
        }
        else {
            appendParamsEncodingClasspath(javaParameters, commandLine, vmParametersList);
        }

        final String mainClass = javaParameters.getMainClass();
        String jarPath = javaParameters.getJarPath();
        if (mainClass != null) {
            commandLine.addParameter(mainClass);
        }
        else if (jarPath != null) {
            commandLine.addParameter("-jar");
            commandLine.addParameter(jarPath);
        }

        commandLine.addParameters(javaParameters.getProgramParametersList().getList());

        commandLine.withWorkDirectory(javaParameters.getWorkingDirectory());

        return commandLine;
    }

    private static void appendParamsEncodingClasspath(SimpleJavaParameters javaParameters,
                                                      GeneralCommandLine commandLine,
                                                      ParametersList parametersList) {
        commandLine.addParameters(parametersList.getList());
        appendEncoding(javaParameters, commandLine, parametersList);
        if (!parametersList.hasParameter("-classpath") && !parametersList.hasParameter("-cp") && !javaParameters.getClassPath().getPathList().isEmpty()){
            commandLine.addParameter("-classpath");
            commandLine.addParameter(javaParameters.getClassPath().getPathsString());
        }
    }

    private static void appendEncoding(SimpleJavaParameters javaParameters, GeneralCommandLine commandLine, ParametersList parametersList) {
        // Value of file.encoding and charset of GeneralCommandLine should be in sync in order process's input and output be correctly handled.
        String encoding = parametersList.getPropertyValue("file.encoding");
        if (encoding == null) {
            Charset charset = javaParameters.getCharset();
            if (charset == null) charset = EncodingManager.getInstance().getDefaultCharset();
            commandLine.addParameter("-Dfile.encoding=" + charset.name());
            commandLine.withCharset(charset);
        }
        else {
            try {
                Charset charset = Charset.forName(encoding);
                commandLine.withCharset(charset);
            }
            catch (UnsupportedCharsetException ignore) { }
            catch (IllegalCharsetNameException ignore) { }
        }
    }

    
    private static Class getCommandLineWrapperClass() {
        try {
            return Class.forName(WRAPPER_CLASS);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static boolean useDynamicClasspath( Project project) {
        final String hasDynamicProperty = System.getProperty("idea.dynamic.classpath", "false");
        return Boolean.valueOf(project != null
                ? PropertiesComponent.getInstance(project).getOrInit("dynamic.classpath", hasDynamicProperty)
                : hasDynamicProperty).booleanValue();
    }

    public static boolean useDynamicVMOptions() {
        return Boolean.valueOf(PropertiesComponent.getInstance().getOrInit("dynamic.vmoptions", "true")).booleanValue();
    }
}