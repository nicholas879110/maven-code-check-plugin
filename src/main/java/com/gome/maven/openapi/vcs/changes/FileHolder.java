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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.openapi.vcs.AbstractVcs;

public interface FileHolder {
    void cleanAll();
    void cleanAndAdjustScope(VcsModifiableDirtyScope scope);
    FileHolder copy();
    HolderType getType();

    void notifyVcsStarted(AbstractVcs scope);

   public static enum HolderType {
        DELETED,
        UNVERSIONED,
        SWITCHED,
        MODIFIED_WITHOUT_EDITING,
        IGNORED,
        LOCKED,
        LOGICALLY_LOCKED,
        ROOT_SWITCH
    }
}
