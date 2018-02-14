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
package com.gome.maven.openapi.application;


import java.awt.*;

/**
 * Represents the stack of active modal dialogs.
 */
public abstract class ModalityState {
     public static final ModalityState NON_MODAL;

    static {
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends ModalityState> ex = (Class<? extends ModalityState>)Class.forName("com.gome.maven.openapi.application.impl.ModalityStateEx");
            NON_MODAL = ex.newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }
    }

    
    public static ModalityState current() {
        return ApplicationManager.getApplication().getCurrentModalityState();
    }

    
    public static ModalityState any() {
        return ApplicationManager.getApplication().getAnyModalityState();
    }

    
    public static ModalityState stateForComponent( Component component){
        return ApplicationManager.getApplication().getModalityStateForComponent(component);
    }

    
    public static ModalityState defaultModalityState() {
        return ApplicationManager.getApplication().getDefaultModalityState();
    }

    public abstract boolean dominates( ModalityState anotherState);

    @Override
    public abstract String toString();
}
