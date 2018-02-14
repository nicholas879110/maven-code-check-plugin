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
package com.gome.maven.psi.impl;

import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.impl.file.impl.FileManager;

/**
 * @author peter
 */
public abstract class PsiManagerEx extends PsiManager {
    public abstract boolean isBatchFilesProcessingMode();

    public abstract boolean isAssertOnFileLoading( VirtualFile file);

    /**
     * @param runnable to be run before <b>physical</b> PSI change
     */
    public abstract void registerRunnableToRunOnChange( Runnable runnable);

    /**
     * @param runnable to be run before <b>physical</b> or <b>non-physical</b> PSI change
     */
    public abstract void registerRunnableToRunOnAnyChange( Runnable runnable);

    public abstract void registerRunnableToRunAfterAnyChange( Runnable runnable);

    
    public abstract FileManager getFileManager();

    public abstract void beforeChildAddition( PsiTreeChangeEventImpl event);

    public abstract void beforeChildRemoval( PsiTreeChangeEventImpl event);

    public abstract void beforeChildReplacement( PsiTreeChangeEventImpl event);

    public abstract void beforeChange(boolean isPhysical);

    public abstract void afterChange(boolean isPhysical);
}
