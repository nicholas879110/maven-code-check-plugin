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

import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.RemoteRevisionsCache;
import com.gome.maven.ui.SimpleColoredComponent;
import com.gome.maven.ui.SimpleTextAttributes;

import java.util.List;

public class RemoteStatusChangeNodeDecorator implements ChangeNodeDecorator {
    protected final RemoteRevisionsCache myRemoteRevisionsCache;

    public RemoteStatusChangeNodeDecorator(final RemoteRevisionsCache remoteRevisionsCache) {
        myRemoteRevisionsCache = remoteRevisionsCache;
    }

    protected void reportState(final boolean state) {
    }

    public void decorate(final Change change, final SimpleColoredComponent component, boolean isShowFlatten) {
        final boolean state = myRemoteRevisionsCache.isUpToDate(change);
        reportState(state);
        if (! state) {
            component.append(" ");
            component.append(VcsBundle.message("change.nodetitle.change.is.outdated"), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }


    public List<Pair<String, Stress>> stressPartsOfFileName(Change change, String parentPath) {
        return null;
    }

    public void preDecorate(Change change, ChangesBrowserNodeRenderer renderer, boolean showFlatten) {
    }
}
