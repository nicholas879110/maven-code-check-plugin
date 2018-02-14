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
package com.gome.maven.openapi.application;

import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.Condition;


public interface ModalityInvokator {
    /**
     * Causes <i>runnable.run()</i> to be executed asynchronously on the
     * AWT event dispatching thread.  This will happen after all
     * pending AWT events have been processed.
     *
     * @param runnable the runnable to execute.
     */
    
    ActionCallback invokeLater( Runnable runnable);

    
    ActionCallback invokeLater( Runnable runnable,  Condition expired);

    /**
     * Causes <i>runnable.run()</i> to be executed asynchronously on the
     * AWT event dispatching thread, when IDEA is in the specified modality
     * state.
     *
     * @param runnable the runnable to execute.
     * @param state the state in which the runnable will be executed.
     */
    
    ActionCallback invokeLater( Runnable runnable,  ModalityState state);

    
    ActionCallback invokeLater( Runnable runnable,  ModalityState state,  Condition expired);
}