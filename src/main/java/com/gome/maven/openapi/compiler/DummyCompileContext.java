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
package com.gome.maven.openapi.compiler;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.CompilerModuleExtension;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.Navigatable;

public class DummyCompileContext implements CompileContext {
    protected DummyCompileContext() {
    }

    private static final DummyCompileContext OUR_INSTANCE = new DummyCompileContext();

    public static DummyCompileContext getInstance() {
        return OUR_INSTANCE;
    }

    public Project getProject() {
        return null;
    }

    public void addMessage(CompilerMessageCategory category, String message, String url, int lineNum, int columnNum) {
    }


    public void addMessage(CompilerMessageCategory category,
                           String message,
                            String url,
                           int lineNum,
                           int columnNum,
                           Navigatable navigatable) {
    }

    public CompilerMessage[] getMessages(CompilerMessageCategory category) {
        return CompilerMessage.EMPTY_ARRAY;
    }

    public int getMessageCount(CompilerMessageCategory category) {
        return 0;
    }

    public ProgressIndicator getProgressIndicator() {
        return null;
    }

    public CompileScope getCompileScope() {
        return null;
    }

    public CompileScope getProjectCompileScope() {
        return null;
    }

    public void requestRebuildNextTime(String message) {
    }

    public boolean isRebuildRequested() {
        return false;
    }

    
    public String getRebuildReason() {
        return null;
    }

    public Module getModuleByFile(VirtualFile file) {
        return null;
    }

    public boolean isAnnotationProcessorsEnabled() {
        return false;
    }

    public VirtualFile[] getSourceRoots(Module module) {
        return VirtualFile.EMPTY_ARRAY;
    }

    public VirtualFile getModuleOutputDirectory(final Module module) {
        return ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
            public VirtualFile compute() {
                return CompilerModuleExtension.getInstance(module).getCompilerOutputPath();
            }
        });
    }

    public VirtualFile getModuleOutputDirectoryForTests(Module module) {
        return null;
    }

    public <T> T getUserData( Key<T> key) {
        return null;
    }

    public <T> void putUserData( Key<T> key, T value) {
    }

    public boolean isMake() {
        return false; // stub implementation
    }

    public boolean isRebuild() {
        return false;
    }
}
