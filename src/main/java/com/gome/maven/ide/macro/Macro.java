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

package com.gome.maven.ide.macro;

import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.io.File;

public abstract class Macro {
    public static final ExtensionPointName<Macro> EP_NAME = ExtensionPointName.create("com.intellij.macro");

    public static final class ExecutionCancelledException extends Exception {
    }

    protected String myCachedPreview;

     public abstract String getName();
    public abstract String getDescription();
    
    public abstract String expand(DataContext dataContext) throws ExecutionCancelledException;

    
    public String expand(DataContext dataContext, String... args) throws ExecutionCancelledException{
        return expand(dataContext);
    }

    public void cachePreview(DataContext dataContext) {
        try{
            myCachedPreview = expand(dataContext);
        }
        catch(ExecutionCancelledException e){
            myCachedPreview = "";
        }
    }

    public final String preview() {
        return myCachedPreview;
    }

    
    static String getPath(VirtualFile file) {
        return file.getPath().replace('/', File.separatorChar);
    }

    static File getIOFile(VirtualFile file) {
        return new File(getPath(file));
    }

    
    protected static VirtualFile getVirtualDirOrParent(DataContext dataContext) {
        VirtualFile vFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        if (vFile != null && !vFile.isDirectory()) {
            vFile = vFile.getParent();
        }
        return vFile;
    }

    public static class Silent extends Macro {
        private final Macro myDelegate;
        private final String myValue;

        public Silent(Macro delegate, String value) {
            myDelegate = delegate;
            myValue = value;
        }

        @Override
        public String expand(DataContext dataContext) throws ExecutionCancelledException {
            return myValue;
        }

        @Override
        public String getDescription() {
            return myDelegate.getDescription();
        }

        @Override
        public String getName() {
            return myDelegate.getName();
        }
    }
}
