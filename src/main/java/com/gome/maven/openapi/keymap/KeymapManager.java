/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.openapi.keymap;

import com.gome.maven.openapi.application.ApplicationManager;

public abstract class KeymapManager {
     public static final String DEFAULT_IDEA_KEYMAP = "$default";
     public static final String MAC_OS_X_KEYMAP = "Mac OS X";
     public static final String X_WINDOW_KEYMAP = "Default for XWin";
     public static final String MAC_OS_X_10_5_PLUS_KEYMAP = "Mac OS X 10.5+";

    public abstract Keymap getActiveKeymap();

    
    public abstract Keymap getKeymap( String name);

    public static KeymapManager getInstance(){
        return ApplicationManager.getApplication().getComponent(KeymapManager.class);
    }

    public abstract void addKeymapManagerListener( KeymapManagerListener listener);

    public abstract void removeKeymapManagerListener( KeymapManagerListener listener);
}
