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
package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.AbstractVcs;
import com.gome.maven.openapi.vcs.ProjectLevelVcsManager;
import com.gome.maven.openapi.vcs.changes.Change;

import java.util.*;

public class AdditionalLocalChangeActionsInstaller {
    
    public static List<AnAction> calculateActions(final Project project, final Collection<Change> changes) {
        final ProjectLevelVcsManager plVcsManager = ProjectLevelVcsManager.getInstance(project);
        final Map<String, AbstractVcs> map = new HashMap<String, AbstractVcs>();
        for (Change change : changes) {
            if (change.getAfterRevision() != null) {
                final AbstractVcs vcs = plVcsManager.getVcsFor(change.getAfterRevision().getFile());
                if ((vcs != null) && (! map.containsKey(vcs.getName()))) {
                    map.put(vcs.getName(), vcs);
                }
            }
        }
        if (map.isEmpty()) {
            return null;
        }
        final List<AnAction> result = new ArrayList<AnAction>(1);
        for (AbstractVcs vcs : map.values()) {
            final List<AnAction> actions = vcs.getAdditionalActionsForLocalChange();
            if (actions != null) {
                result.addAll(actions);
            }
        }
        return result;
    }
}
