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


public interface ChangeListModification {
    LocalChangeList addChangeList( String name,  final String comment);
    void setDefaultChangeList( LocalChangeList list);

    void removeChangeList(final String name);
    void removeChangeList(final LocalChangeList list);

    void moveChangesTo(final LocalChangeList list, final Change... changes);

    // added - since ChangeListManager wouldn't pass internal lists, only copies
    boolean setReadOnly(final String name, final boolean value);

    boolean editName( String fromName,  String toName);
    
    String editComment( String fromName, final String newComment);
}
