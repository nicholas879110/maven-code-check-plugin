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

package com.gome.maven.codeInsight.lookup;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;

import java.beans.PropertyChangeListener;

public abstract class LookupManager {
    public static LookupManager getInstance(Project project){
        return ServiceManager.getService(project, LookupManager.class);
    }

    
    public static LookupEx getActiveLookup( Editor editor) {
        if (editor == null) return null;

        final Project project = editor.getProject();
        if (project == null || project.isDisposed()) return null;

        final LookupEx lookup = getInstance(project).getActiveLookup();
        if (lookup == null) return null;

        return InjectedLanguageUtil.getTopLevelEditor(lookup.getEditor()) == InjectedLanguageUtil.getTopLevelEditor(editor) ? lookup : null;
    }

    
    public LookupEx showLookup( Editor editor,  LookupElement... items) {
        return showLookup(editor, items, "", new LookupArranger.DefaultArranger());
    }

    
    public LookupEx showLookup( Editor editor,  LookupElement[] items,  String prefix) {
        return showLookup(editor, items, prefix, new LookupArranger.DefaultArranger());
    }

    
    public abstract LookupEx showLookup( Editor editor,
                                         LookupElement[] items,
                                         String prefix,
                                         LookupArranger arranger);

    public abstract void hideActiveLookup();

    
    public abstract LookupEx getActiveLookup();

     public static final String PROP_ACTIVE_LOOKUP = "activeLookup";

    public abstract void addPropertyChangeListener( PropertyChangeListener listener);
    public abstract void addPropertyChangeListener( PropertyChangeListener listener,  Disposable disposable);
    public abstract void removePropertyChangeListener( PropertyChangeListener listener);

    
    public abstract Lookup createLookup( Editor editor,  LookupElement[] items,  final String prefix,  LookupArranger arranger);

}