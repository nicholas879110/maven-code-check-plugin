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

package com.gome.maven.history.core.tree;

import com.gome.maven.history.core.StreamUtil;
import com.gome.maven.history.core.revisions.Difference;
import com.gome.maven.history.utils.LocalHistoryLog;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DirectoryEntry extends Entry {
    private final ArrayList<Entry> myChildren;

    public DirectoryEntry(String name) {
        super(name);
        myChildren = new ArrayList<Entry>(3);
    }

    public DirectoryEntry(DataInput in, boolean dummy /* to distinguish from general constructor*/) throws IOException {
        super(in);
        int count = in.readInt();
        myChildren = new ArrayList<Entry>(count);
        while (count-- > 0) {
            unsafeAddChild(StreamUtil.readEntry(in));
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(myChildren.size());
        for (Entry child : myChildren) {
            StreamUtil.writeEntry(out, child);
        }
    }

    @Override
    public long getTimestamp() {
        return -1;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public void addChild(Entry child) {
        if (!checkDoesNotExist(child, child.getName())) return;
        unsafeAddChild(child);
    }

    public void addChildren(Collection<Entry> children) {
        myChildren.ensureCapacity(myChildren.size() + children.size());
        for (Entry each : children) {
            unsafeAddChild(each);
        }
    }

    private void unsafeAddChild(Entry child) {
        myChildren.add(child);
        child.setParent(this);
    }

    protected boolean checkDoesNotExist(Entry e, String name) {
        Entry found = findChild(name);
        if (found == null) return true;
        if (found == e) return false;

        removeChild(found);
        LocalHistoryLog.LOG.warn(String.format("entry '%s' already exists in '%s'", name, getPath()));
        return true;
    }

    @Override
    public void removeChild(Entry child) {
        myChildren.remove(child);
        child.setParent(null);
    }

    @Override
    public List<Entry> getChildren() {
        return myChildren;
    }

    @Override
    public boolean hasUnavailableContent(List<Entry> entriesWithUnavailableContent) {
        for (Entry e : myChildren) {
            e.hasUnavailableContent(entriesWithUnavailableContent);
        }
        return !entriesWithUnavailableContent.isEmpty();
    }

    @Override
    public DirectoryEntry copy() {
        DirectoryEntry result = copyEntry();
        result.myChildren.ensureCapacity(myChildren.size());
        for (Entry child : myChildren) {
            result.unsafeAddChild(child.copy());
        }
        return result;
    }

    protected DirectoryEntry copyEntry() {
        return new DirectoryEntry(myName);
    }

    @Override
    public void collectDifferencesWith(Entry right, List<Difference> result) {
        DirectoryEntry e = (DirectoryEntry)right;

        if (!getPath().equals(e.getPath())) {
            result.add(new Difference(false, this, e));
        }

        addCreatedChildrenDifferences(e, result);
        addDeletedChildrenDifferences(e, result);
        addModifiedChildrenDifferences(e, result);
    }

    private void addCreatedChildrenDifferences(DirectoryEntry e, List<Difference> result) {
        for (Entry child : e.myChildren) {
            if (findDirectChild(child.getName(), child.isDirectory()) == null) {
                child.collectCreatedDifferences(result);
            }
        }
    }

    private void addDeletedChildrenDifferences(DirectoryEntry e, List<Difference> result) {
        for (Entry child : myChildren) {
            if (e.findDirectChild(child.getName(), child.isDirectory()) == null) {
                child.collectDeletedDifferences(result);
            }
        }
    }

    private void addModifiedChildrenDifferences(DirectoryEntry e, List<Difference> result) {
        for (Entry myChild : myChildren) {
            Entry itsChild = e.findDirectChild(myChild.getName(), myChild.isDirectory());
            if (itsChild != null) {
                myChild.collectDifferencesWith(itsChild, result);
            }
        }
    }

    Entry findDirectChild(String name, boolean isDirectory) {
        for (Entry child : getChildren()) {
            if (child.isDirectory() == isDirectory && child.nameEquals(name)) return child;
        }
        return null;
    }

    @Override
    protected void collectCreatedDifferences(List<Difference> result) {
        result.add(new Difference(false, null, this));

        for (Entry child : myChildren) {
            child.collectCreatedDifferences(result);
        }
    }

    @Override
    protected void collectDeletedDifferences(List<Difference> result) {
        result.add(new Difference(false, this, null));

        for (Entry child : myChildren) {
            child.collectDeletedDifferences(result);
        }
    }
}
