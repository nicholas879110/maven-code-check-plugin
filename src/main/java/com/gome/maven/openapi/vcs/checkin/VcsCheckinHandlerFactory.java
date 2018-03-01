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
package com.gome.maven.openapi.vcs.checkin;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.CheckinProjectPanel;
import com.gome.maven.openapi.vcs.VcsKey;
import com.gome.maven.openapi.vcs.changes.CommitContext;

public abstract class VcsCheckinHandlerFactory implements BaseCheckinHandlerFactory {
    public static final ExtensionPointName<VcsCheckinHandlerFactory> EP_NAME = ExtensionPointName.create("com.gome.maven.vcsCheckinHandlerFactory");

    private VcsKey myKey;

    protected VcsCheckinHandlerFactory( final VcsKey key) {
        myKey = key;
    }

    
    @Override
    public CheckinHandler createHandler( CheckinProjectPanel panel,  CommitContext commitContext) {
        if (! panel.vcsIsAffected(myKey.getName())) return CheckinHandler.DUMMY;
        return createVcsHandler(panel);
    }

    
    protected abstract CheckinHandler createVcsHandler(CheckinProjectPanel panel);

    public VcsKey getKey() {
        return myKey;
    }

    @Override
    public BeforeCheckinDialogHandler createSystemReadyHandler( Project project) {
        return null;
    }
}
