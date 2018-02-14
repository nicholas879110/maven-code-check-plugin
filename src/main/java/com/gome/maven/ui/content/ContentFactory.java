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
package com.gome.maven.ui.content;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

import javax.swing.*;

public interface ContentFactory {
    
    Content createContent(JComponent component, String displayName, boolean isLockable);

    
    ContentManager createContentManager( ContentUI contentUI, boolean canCloseContents,  Project project);

    
    ContentManager createContentManager(boolean canCloseContents,  Project project);

    class SERVICE {
        private SERVICE() {
        }

        public static ContentFactory getInstance() {
            return ServiceManager.getService(ContentFactory.class);
        }
    }
}
