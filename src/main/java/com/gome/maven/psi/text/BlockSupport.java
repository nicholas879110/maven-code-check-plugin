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

package com.gome.maven.psi.text;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.text.DiffLog;
import com.gome.maven.util.IncorrectOperationException;

public abstract class BlockSupport {
    public static BlockSupport getInstance(Project project) {
        return ServiceManager.getService(project, BlockSupport.class);
    }

    public abstract void reparseRange(PsiFile file, int startOffset, int endOffset,  CharSequence newText) throws IncorrectOperationException;

    
    public abstract DiffLog reparseRange( PsiFile file,
                                          TextRange changedPsiRange,
                                          CharSequence newText,
                                          ProgressIndicator progressIndicator) throws IncorrectOperationException;

    public static final Key<Boolean> DO_NOT_REPARSE_INCREMENTALLY = Key.create("DO_NOT_REPARSE_INCREMENTALLY");
    public static final Key<ASTNode> TREE_TO_BE_REPARSED = Key.create("TREE_TO_BE_REPARSED");

    public static class ReparsedSuccessfullyException extends RuntimeException {
        private final DiffLog myDiffLog;

        public ReparsedSuccessfullyException( DiffLog diffLog) {
            myDiffLog = diffLog;
        }

        
        public DiffLog getDiffLog() {
            return myDiffLog;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    // maximal tree depth for which incremental reparse is allowed
    // if tree is deeper then it will be replaced completely - to avoid SOEs
    public static final int INCREMENTAL_REPARSE_DEPTH_LIMIT = Registry.intValue("psi.incremental.reparse.depth.limit");

    public static final Key<Boolean> TREE_DEPTH_LIMIT_EXCEEDED = Key.create("TREE_IS_TOO_DEEP");

    public static boolean isTooDeep(final UserDataHolder element) {
        return element != null && Boolean.TRUE.equals(element.getUserData(TREE_DEPTH_LIMIT_EXCEEDED));
    }
}
