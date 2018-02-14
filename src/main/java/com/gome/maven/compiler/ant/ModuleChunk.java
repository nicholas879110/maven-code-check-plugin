/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.compiler.ant;

import com.gome.maven.compiler.CompilerConfiguration;
import com.gome.maven.compiler.CompilerEncodingService;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.module.EffectiveLanguageLevelUtil;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.CompilerModuleExtension;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.pom.java.LanguageLevel;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Module chunk consists of interdependent modules.
 *
 * @author Eugene Zhuravlev
 *         Date: Nov 19, 2004
 */
public class ModuleChunk {
    /**
     * Modules in the chunk
     */
    private final Module[] myModules;
    /**
     * A array of custom compilation providers.
     */
    private final ChunkCustomCompilerExtension[] myCustomCompilers;
    /**
     * The main module in the chunck (guessed by heuristic or selected by user)
     */
    private Module myMainModule;
    /**
     * Chucnk dependendencies
     */
    private ModuleChunk[] myDependentChunks;
    private File myBaseDir = null;

    public ModuleChunk(Module[] modules) {
        myModules = modules;
        Arrays.sort(myModules, new Comparator<Module>() {
            public int compare(final Module o1, final Module o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        myMainModule = myModules[0];
        myCustomCompilers = ChunkCustomCompilerExtension.getCustomCompile(this);
    }

    public String getName() {
        return myMainModule.getName();
    }

    /**
     * @return an array of custom compilers for the module chunk
     */
    public ChunkCustomCompilerExtension[] getCustomCompilers() {
        return myCustomCompilers;
    }

    public Module[] getModules() {
        return myModules;
    }

    
    public String getOutputDirUrl() {
        return CompilerModuleExtension.getInstance(myMainModule).getCompilerOutputUrl();
    }

    
    public String getTestsOutputDirUrl() {
        return CompilerModuleExtension.getInstance(myMainModule).getCompilerOutputUrlForTests();
    }

    public boolean isJdkInherited() {
        return ModuleRootManager.getInstance(myMainModule).isSdkInherited();
    }

    
    public Sdk getJdk() {
        return ModuleRootManager.getInstance(myMainModule).getSdk();
    }

    public ModuleChunk[] getDependentChunks() {
        return myDependentChunks;
    }

    public void setDependentChunks(ModuleChunk[] dependentChunks) {
        myDependentChunks = dependentChunks;
    }

    public File getBaseDir() {
        if (myBaseDir != null) {
            return myBaseDir;
        }
        return new File(myMainModule.getModuleFilePath()).getParentFile();
    }

    public void setBaseDir(File baseDir) {
        myBaseDir = baseDir;
    }

    public void setMainModule(Module module) {
        myMainModule = module;
    }

    public Project getProject() {
        return myMainModule.getProject();
    }

    public String getChunkSpecificCompileOptions() {
        final StringBuilder options = new StringBuilder();
        final Charset encoding = CompilerEncodingService.getInstance(getProject()).getPreferredModuleEncoding(myMainModule);
        if (encoding != null) {
            appendOption(options, "-encoding", encoding.name());
        }
        appendOption(options, "-source", getLanguageLevelOption(ApplicationManager.getApplication().runReadAction(new Computable<LanguageLevel>() {
            @Override
            public LanguageLevel compute() {
                return EffectiveLanguageLevelUtil.getEffectiveLanguageLevel(myMainModule);
            }
        })));
        appendOption(options, "-target", CompilerConfiguration.getInstance(getProject()).getBytecodeTargetLevel(myMainModule));
        return options.toString();
    }


    public boolean contains(final Module module) {
        for (Module chunkModule : myModules) {
            if (chunkModule.equals(module)) {
                return true;
            }
        }
        return false;
    }

    private static void appendOption(StringBuilder options,  final String name,  String value) {
        if (!StringUtil.isEmpty(value)) {
            if (options.length() > 0) {
                options.append(" ");
            }
            options.append(name).append(" ").append(value);
        }
    }

    private static String getLanguageLevelOption(LanguageLevel level) {
        if (level != null) {
            switch (level) {
                case JDK_1_3: return "1.3";
                case JDK_1_4: return "1.4";
                case JDK_1_5: return "1.5";
                case JDK_1_6: return "1.6";
                case JDK_1_7: return "1.7";
                case JDK_1_8: return "8";
                case JDK_1_9: return "9";
            }
        }
        return null;
    }

}
