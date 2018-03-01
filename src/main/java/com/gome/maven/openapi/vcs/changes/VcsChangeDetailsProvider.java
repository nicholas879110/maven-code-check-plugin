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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.progress.BackgroundTaskQueue;
import com.gome.maven.openapi.util.Pair;

import javax.swing.*;

/**
 * @author irengrig
 *         Date: 7/5/11
 *         Time: 2:49 PM
 */
public interface VcsChangeDetailsProvider {
    ExtensionPointName<VcsChangeDetailsProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.vcschangedetails");

    String getName();

//    @CalledInAwt
    boolean canComment(final Change change);
//    @CalledInAwt
    RefreshablePanel comment(final Change change, JComponent parent, BackgroundTaskQueue queue);
}
