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
package com.gome.maven.openapi.diff.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.DiffContent;
import com.gome.maven.openapi.diff.impl.highlighting.FragmentSide;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.fileEditor.FileEditor;

/**
 * @author Konstantin Bulenkov
 * @author max
 */
public interface EditorSource {
     FragmentSide getSide();

     DiffContent getContent();

     EditorEx getEditor();

     FileEditor getFileEditor();

    void addDisposable( Disposable disposable);

    EditorSource NULL = new EditorSource() {
        public EditorEx getEditor() {
            return null;
        }

        @Override
        public FileEditor getFileEditor() {
            return null;
        }

        public void addDisposable( Disposable disposable) {
            Logger.getInstance("#com.intellij.openapi.diff.impl.EditorSource").assertTrue(false);
        }

        
        public FragmentSide getSide() {
            return null;
        }

        
        public DiffContent getContent() {
            return null;
        }
    };
}
