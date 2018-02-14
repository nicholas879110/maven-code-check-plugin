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

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.actions.VcsContextFactory;

import java.util.Collection;

/**
 * @author max
 */
public abstract class LocalChangeList implements Cloneable, ChangeList {

     public static final String DEFAULT_NAME = VcsBundle.message("changes.default.changelist.name");

    public static LocalChangeList createEmptyChangeList(Project project,  String name) {
        return VcsContextFactory.SERVICE.getInstance().createLocalChangeList(project, name);
    }

    public abstract Collection<Change> getChanges();

    /**
     * Logical id that identifies the changelist and should survive name changing.
     * @return changelist id
     */
    
    public String getId() {
        return getName();
    }

    
    public abstract String getName();

    public abstract void setName( String name);

    
    public abstract String getComment();

    public abstract void setComment( String comment);

    public abstract boolean isDefault();

    public abstract boolean isReadOnly();

    public abstract void setReadOnly(boolean isReadOnly);

    /**
     * Get additional data associated with this changelist.
     */
    
    public abstract Object getData();

    public abstract LocalChangeList copy();

    public boolean hasDefaultName() {
        return DEFAULT_NAME.equals(getName());
    }
}
