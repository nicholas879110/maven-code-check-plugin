/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.openapi.compiler;

import com.gome.maven.compiler.CompilerConfiguration;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectUtilCore;
import com.gome.maven.openapi.roots.CompilerModuleExtension;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.util.PathUtil;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * A set of utility methods for working with paths
 */
public class CompilerPaths {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.compiler.CompilerPaths");
    private static volatile String ourSystemPath;
    private static final Comparator<String> URLS_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    };
    /**
     * Returns a directory
     * @return a directory where compiler may generate files. All generated files are not deleted when the application exits
     */
    public static File getGeneratedDataDirectory(Project project, Compiler compiler) {
        //noinspection HardCodedStringLiteral
        return new File(getGeneratedDataDirectory(project), compiler.getDescription().replaceAll("\\s+", "_"));
    }

    /**
     * @return a root directory where generated files for various compilers are stored
     */
    public static File getGeneratedDataDirectory(Project project) {
        //noinspection HardCodedStringLiteral
        return new File(getCompilerSystemDirectory(project), ".generated");
    }

    /**
     * @return a root directory where compiler caches for the given project are stored
     */
    public static File getCacheStoreDirectory(final Project project) {
        //noinspection HardCodedStringLiteral
        return new File(getCompilerSystemDirectory(project), ".caches");
    }

    public static File getCacheStoreDirectory(String compilerProjectDirName) {
        //noinspection HardCodedStringLiteral
        return new File(getCompilerSystemDirectory(compilerProjectDirName), ".caches");
    }

    public static File getRebuildMarkerFile(Project project) {
        return new File(getCompilerSystemDirectory(project), "rebuild_required");
    }

    /**
     * @return a directory under IDEA "system" directory where all files related to compiler subsystem are stored (such as compiler caches or generated files)
     */
    public static File getCompilerSystemDirectory(Project project) {
        return getCompilerSystemDirectory(getCompilerSystemDirectoryName(project));
    }

    public static File getCompilerSystemDirectory(String compilerProjectDirName) {
        return new File(getCompilerSystemDirectory(), compilerProjectDirName);
    }

    public static String getCompilerSystemDirectoryName(Project project) {
        return ProjectUtilCore.getPresentableName(project) + "." + project.getLocationHash();
    }

    public static File getCompilerSystemDirectory() {
        //noinspection HardCodedStringLiteral
        final String systemPath = ourSystemPath != null? ourSystemPath : (ourSystemPath = PathUtil.getCanonicalPath(PathManager.getSystemPath()));
        return new File(systemPath, "compiler");
    }

    /**
     * @param forTestClasses true if directory for test sources, false - for sources.
     * @return a directory to which the sources (or test sources depending on the second partameter) should be compiled.
     * Null is returned if output directory is not specified or is not valid
     */
    
    public static VirtualFile getModuleOutputDirectory(final Module module, boolean forTestClasses) {
        final CompilerModuleExtension compilerModuleExtension = CompilerModuleExtension.getInstance(module);
        VirtualFile outPath;
        if (forTestClasses) {
            final VirtualFile path = compilerModuleExtension.getCompilerOutputPathForTests();
            if (path != null) {
                outPath = path;
            }
            else {
                outPath = compilerModuleExtension.getCompilerOutputPath();
            }
        }
        else {
            outPath = compilerModuleExtension.getCompilerOutputPath();
        }
        if (outPath == null) {
            return null;
        }
        if (!outPath.isValid()) {
            LOG.info("Requested output path for module " + module.getName() + " is not valid");
            return null;
        }
        return outPath;
    }

    /**
     * The same as {@link #getModuleOutputDirectory} but returns String.
     * The method still returns a non-null value if the output path is specified in Settings but does not exist on disk.
     */
    
    public static String getModuleOutputPath(final Module module, boolean forTestClasses) {
        final String outPathUrl;
        final Application application = ApplicationManager.getApplication();
        final CompilerModuleExtension extension = CompilerModuleExtension.getInstance(module);
        if (forTestClasses) {
            if (application.isDispatchThread()) {
                final String url = extension.getCompilerOutputUrlForTests();
                outPathUrl = url != null ? url : extension.getCompilerOutputUrl();
            }
            else {
                outPathUrl = application.runReadAction(new Computable<String>() {
                    @Override
                    public String compute() {
                        final String url = extension.getCompilerOutputUrlForTests();
                        return url != null ? url : extension.getCompilerOutputUrl();
                    }
                });
            }
        }
        else { // for ordinary classes
            if (application.isDispatchThread()) {
                outPathUrl = extension.getCompilerOutputUrl();
            }
            else {
                outPathUrl = application.runReadAction(new Computable<String>() {
                    @Override
                    public String compute() {
                        return extension.getCompilerOutputUrl();
                    }
                });
            }
        }
        return outPathUrl != null? VirtualFileManager.extractPath(outPathUrl) : null;
    }

    
    public static String getAnnotationProcessorsGenerationPath(Module module) {
        final AnnotationProcessingConfiguration config = CompilerConfiguration.getInstance(module.getProject()).getAnnotationProcessingConfiguration(module);
        final String sourceDirName = config.getGeneratedSourcesDirectoryName(false);
        if (config.isOutputRelativeToContentRoot()) {
            final String[] roots = ModuleRootManager.getInstance(module).getContentRootUrls();
            if (roots.length == 0) {
                return null;
            }
            if (roots.length > 1) {
                Arrays.sort(roots, URLS_COMPARATOR);
            }
            return StringUtil.isEmpty(sourceDirName)? VirtualFileManager.extractPath(roots[0]): VirtualFileManager.extractPath(roots[0]) + "/" + sourceDirName;
        }


        final String path = getModuleOutputPath(module, false);
        if (path == null) {
            return null;
        }
        return StringUtil.isEmpty(sourceDirName)? path : path + "/" + sourceDirName;
    }

}
