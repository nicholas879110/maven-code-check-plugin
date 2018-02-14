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
package com.gome.maven.openapi.preview;

import com.gome.maven.openapi.project.Project;

public interface PreviewManager {
    class SERVICE {

        private SERVICE() {
        }

        
        private static PreviewManager getInstance( Project project) {
            return null;//disabled for a while
            //if (!UISettings.getInstance().NAVIGATE_TO_PREVIEW) return null;
            //return ServiceManager.getService(project, PreviewManager.class);
        }

        /**
         * @return null if preview cannot be performed
         */
        
        public static <V, C> C preview( Project project,  PreviewProviderId<V, C> id, V data, boolean requestFocus) {
            PreviewManager instance = getInstance(project);
            if (instance == null) return null;
            return instance.preview(id, data, requestFocus);
        }

        public static <V, C> void close( Project project,  PreviewProviderId<V, C> id, V data) {
            PreviewManager instance = getInstance(project);
            if (instance != null) {
                instance.close(id, data);
            }
        }

        public static <V, C> void moveToStandardPlaceImpl( Project project,  PreviewProviderId<V, C> id, V data) {
            PreviewManager instance = getInstance(project);
            if (instance != null) {
                instance.moveToStandardPlaceImpl(id, data);
            }
        }
    }

    /**
     * @return <code>null</code> if provider is not available / not active or if it forces to use standard view instead of preview at the moment
     */
    
    <V, C> C preview( PreviewProviderId<V, C> id, V data, boolean requestFocus);

    <V, C> void moveToStandardPlaceImpl( PreviewProviderId<V, C> id, V data);

    <V, C> void close( PreviewProviderId<V, C> id, V data);
}
