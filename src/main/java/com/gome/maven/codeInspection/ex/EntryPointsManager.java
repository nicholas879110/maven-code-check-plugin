/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/*
 * User: anna
  * Date: 28-Feb-2007
  */
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefManager;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class EntryPointsManager implements Disposable {
    public static EntryPointsManager getInstance(Project project) {
        return ServiceManager.getService(project, EntryPointsManager.class);
    }

    public abstract void resolveEntryPoints( RefManager manager);

    public abstract void addEntryPoint( RefElement newEntryPoint, boolean isPersistent);

    public abstract void removeEntryPoint( RefElement anEntryPoint);

    
    public abstract RefElement[] getEntryPoints();

    public abstract void cleanup();

    public abstract boolean isAddNonJavaEntries();

    public abstract void configureAnnotations();

    /**
     * {@link com.gome.maven.codeInspection.ex.EntryPointsManagerImpl#createConfigureAnnotationsButton()} should be used instead
     */
    @Deprecated
    public abstract JButton createConfigureAnnotationsBtn();

    public abstract boolean isEntryPoint( PsiElement element);
}
