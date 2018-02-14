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
package com.gome.maven.openapi.editor.ex;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.event.EditorEventMulticaster;

import java.beans.PropertyChangeListener;

public interface EditorEventMulticasterEx extends EditorEventMulticaster{
    void addErrorStripeListener( ErrorStripeListener listener);
    void addErrorStripeListener( ErrorStripeListener listener,  Disposable parentDisposable);
    void removeErrorStripeListener( ErrorStripeListener listener);

    void addEditReadOnlyListener( EditReadOnlyListener listener);
    void removeEditReadOnlyListener( EditReadOnlyListener listener);

    void addPropertyChangeListener( PropertyChangeListener listener);
    void removePropertyChangeListener( PropertyChangeListener listener);

    void addFocusChangeListner( FocusChangeListener listener);
    void addFocusChangeListner( FocusChangeListener listener,  Disposable parentDisposable);
    void removeFocusChangeListner( FocusChangeListener listener);
}
