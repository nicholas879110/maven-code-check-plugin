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
package com.gome.maven.openapi.fileChooser;

import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Consumer;

import java.util.List;

/**
 * @author Roman Shevchenko
 * @since 13.02.2012
 */
public interface PathChooserDialog {
    DataKey<Boolean> PREFER_LAST_OVER_EXPLICIT = DataKey.create("prefer.last.over.explicit");

    /** @deprecated use {@link FileChooserDescriptor#withShowHiddenFiles(boolean)} (to be removed in IDEA 15) */
    DataKey<Boolean> NATIVE_MAC_CHOOSER_SHOW_HIDDEN_FILES = DataKey.create("native.mac.chooser.show.hidden.files");

    void choose( VirtualFile toSelect,  final Consumer<List<VirtualFile>> callback);
}
