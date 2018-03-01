/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.psi.impl.cache;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.UsageSearchContext;
import com.gome.maven.util.Processor;
//import org.gome.maven.lang.annotations.MagicConstant;

public interface CacheManager {
    class SERVICE {
        private SERVICE() {
        }

        public static CacheManager getInstance(Project project) {
            return ServiceManager.getService(project, CacheManager.class);
        }
    }

    
    PsiFile[] getFilesWithWord( String word, short occurenceMask,  GlobalSearchScope scope, final boolean caseSensitively);

    
    VirtualFile[] getVirtualFilesWithWord( String word, short occurenceMask,  GlobalSearchScope scope, final boolean caseSensitively);

    boolean processFilesWithWord( Processor<PsiFile> processor,
                                  String word,
                                 /*@MagicConstant(flagsFromClass = UsageSearchContext.class) */short occurenceMask,
                                  GlobalSearchScope scope,
                                 final boolean caseSensitively);
}

