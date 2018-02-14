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

package com.gome.maven.codeInspection.ui;

import com.gome.maven.openapi.vcs.FileStatus;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

/**
 * @author max
 */
public abstract class InspectionTreeNode extends DefaultMutableTreeNode {
    private boolean myResolved = false;
    protected InspectionTreeNode(Object userObject) {
        super(userObject);
    }

    public abstract Icon getIcon(boolean expanded);

    public int getProblemCount() {
        int sum = 0;
        Enumeration enumeration = children();
        while (enumeration.hasMoreElements()) {
            InspectionTreeNode child = (InspectionTreeNode)enumeration.nextElement();
            sum += child.getProblemCount();
        }
        return sum;
    }

    public boolean isValid() {
        return true;
    }

    public boolean isResolved(){
        return myResolved;
    }

    public boolean appearsBold() {
        return false;
    }

    public FileStatus getNodeStatus(){
        return FileStatus.NOT_CHANGED;
    }

    public void ignoreElement() {
        myResolved = true;
        Enumeration enumeration = children();
        while (enumeration.hasMoreElements()) {
            InspectionTreeNode child = (InspectionTreeNode)enumeration.nextElement();
            child.ignoreElement();
        }
    }

    public void amnesty() {
        myResolved = false;
        Enumeration enumeration = children();
        while (enumeration.hasMoreElements()) {
            InspectionTreeNode child = (InspectionTreeNode)enumeration.nextElement();
            child.amnesty();
        }
    }
}
