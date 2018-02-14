/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff;

import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

import java.awt.*;

public abstract class DiffManager {
    public static DiffManager getInstance() {
        return ServiceManager.getService(DiffManager.class);
    }

    //
    // Usage
    //

    public abstract void showDiff( Project project,  DiffRequest request);

    public abstract void showDiff( Project project,  DiffRequest request,  DiffDialogHints hints);

    public abstract void showDiff( Project project,  DiffRequestChain requests,  DiffDialogHints hints);

    
    public abstract DiffRequestPanel createRequestPanel( Project project,  Disposable parent,  Window window);
}
