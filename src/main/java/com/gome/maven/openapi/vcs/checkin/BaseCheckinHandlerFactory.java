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

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.CheckinProjectPanel;
import com.gome.maven.openapi.vcs.changes.CommitContext;

public interface BaseCheckinHandlerFactory {
    /**
     * Creates a handler for a single Checkin Project or Checkin File operation.
     *
     * @param panel the class which can be used to retrieve information about the files to be committed,
     *              and to get or set the commit message.
     * @return the handler instance.
     */
    
    CheckinHandler createHandler( CheckinProjectPanel panel,  CommitContext commitContext);

    
    BeforeCheckinDialogHandler createSystemReadyHandler( Project project);
}
