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
package com.gome.maven.openapi.actionSystem;


/**
 * Allows an action to retrieve information about the context in which it was invoked.
 *
 * @see com.gome.maven.openapi.actionSystem.AnActionEvent#getDataContext()
 * @see com.gome.maven.openapi.actionSystem.PlatformDataKeys
 * @see com.gome.maven.openapi.actionSystem.DataKey
 * @see com.gome.maven.ide.DataManager
 * @see DataProvider
 */
public interface DataContext {
    /**
     * Returns the object corresponding to the specified data identifier. Some of the supported
     * data identifiers are defined in the {@link com.gome.maven.openapi.actionSystem.PlatformDataKeys} class.
     *
     * @param dataId the data identifier for which the value is requested.
     * @return the value, or null if no value is available in the current context for this identifier.
     */
    Object getData( String dataId);

    DataContext EMPTY_CONTEXT = new DataContext() {
        @Override
        public Object getData( String dataId) {
            return null;
        }
    };
}
