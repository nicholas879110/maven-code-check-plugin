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

/*
 * @author max
 */
package com.gome.maven.openapi.application.impl;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityInvokator;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.Condition;

class ModalityInvokatorImpl implements ModalityInvokator {
    ModalityInvokatorImpl() {
    }

    
    @Override
    public ActionCallback invokeLater( Runnable runnable) {
        return invokeLater(runnable, ApplicationManager.getApplication().getDisposed());
    }

    
    @Override
    public ActionCallback invokeLater( final Runnable runnable,  final Condition expired) {
        return LaterInvocator.invokeLater(runnable, expired);
    }

    
    @Override
    public ActionCallback invokeLater( final Runnable runnable,  final ModalityState state,  final Condition expired) {
        return LaterInvocator.invokeLater(runnable, state, expired);
    }

    
    @Override
    public ActionCallback invokeLater( Runnable runnable,  ModalityState state) {
        return invokeLater(runnable, state, ApplicationManager.getApplication().getDisposed());
    }
}