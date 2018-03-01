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
package com.gome.maven.refactoring.move.moveClassesOrPackages;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.psi.PsiClass;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.IncorrectOperationException;

import java.util.Collection;

/**
 * @author Maxim.Medvedev
 */
public interface MoveClassHandler {
    ExtensionPointName<MoveClassHandler> EP_NAME = new ExtensionPointName<MoveClassHandler>("com.gome.maven.refactoring.moveClassHandler");

    void prepareMove( PsiClass aClass);

    void finishMoveClass( PsiClass aClass);

    /**
     * @return null if it cannot move aClass
     */
    
    PsiClass doMoveClass( PsiClass aClass,  PsiDirectory moveDestination) throws IncorrectOperationException;

    /**
     *
     * @param clazz psiClass
     * @return null, if this instance of FileNameForPsiProvider cannot provide name for clazz
     */
    String getName(PsiClass clazz);

    void preprocessUsages(Collection<UsageInfo> results);
}
