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
package com.gome.maven.openapi.editor.event;


import java.util.EventListener;

/**
 * @see {@link com.gome.maven.openapi.editor.EditorFactory#addEditorFactoryListener(com.gome.maven.openapi.editor.event.EditorFactoryListener, com.gome.maven.openapi.Disposable)}
 */
public interface EditorFactoryListener extends EventListener {
    /**
     * Called after {@link com.gome.maven.openapi.editor.Editor} instance has been created.
     */
    void editorCreated( EditorFactoryEvent event);
    /**
     * Called before {@link com.gome.maven.openapi.editor.Editor} instance will be released.
     */
    void editorReleased( EditorFactoryEvent event);
}

