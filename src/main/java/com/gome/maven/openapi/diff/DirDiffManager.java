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
package com.gome.maven.openapi.diff;

import com.gome.maven.ide.diff.DiffElement;
import com.gome.maven.ide.diff.DirDiffModel;
import com.gome.maven.ide.diff.DirDiffSettings;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

/**
 * @author Konstantin Bulenkov
 */
public abstract class DirDiffManager {
    public static DirDiffManager getInstance( Project project) {
        return ServiceManager.getService(project, DirDiffManager.class);
    }

    public abstract void showDiff( DiffElement dir1,  DiffElement dir2, DirDiffSettings settings,  Runnable onWindowClose);

    public abstract void showDiff( DiffElement dir1,  DiffElement dir2, DirDiffSettings settings);

    public abstract void showDiff( DiffElement dir1,  DiffElement dir2);

    public abstract boolean canShow( DiffElement dir1,  DiffElement dir2);

    
    public abstract DiffElement createDiffElement(Object obj);

    public abstract DirDiffModel createDiffModel(DiffElement e1, DiffElement e2, DirDiffSettings settings);
}
