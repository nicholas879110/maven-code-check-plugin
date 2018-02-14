/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.util.IconUtil;

import javax.swing.*;
import java.util.Enumeration;

/**
 * @author max
 */
public class InspectionNode extends InspectionTreeNode {
    public static final Icon TOOL = LayeredIcon.create(AllIcons.Toolwindows.ToolWindowInspection, IconUtil.getEmptyIcon(false));
    private boolean myTooBigForOnlineRefresh = false;

    public InspectionNode( InspectionToolWrapper toolWrapper) {
        super(toolWrapper);
    }

    public String toString() {
        return getToolWrapper().getDisplayName();
    }

    
    public InspectionToolWrapper getToolWrapper() {
        return (InspectionToolWrapper)getUserObject();
    }

    @Override
    public Icon getIcon(boolean expanded) {
        return TOOL;
    }

    public boolean isTooBigForOnlineRefresh() {
        if(!myTooBigForOnlineRefresh) myTooBigForOnlineRefresh = getProblemCount()>1000;
        return myTooBigForOnlineRefresh;
    }

    @Override
    public int getProblemCount() {
        int sum = 0;
        Enumeration children = children();
        while (children.hasMoreElements()) {
            InspectionTreeNode child = (InspectionTreeNode)children.nextElement();
            if (child instanceof InspectionNode) continue;
            sum += child.getProblemCount();
        }
        return sum;
    }
}
