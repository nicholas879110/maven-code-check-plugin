/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.ui.treeStructure;

import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.util.treeView.NodeDescriptor;
import com.gome.maven.ide.util.treeView.PresentableNodeDescriptor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.update.ComparableObject;
import com.gome.maven.util.ui.update.ComparableObjectCheck;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class SimpleNode extends PresentableNodeDescriptor implements ComparableObject {

    protected static final SimpleNode[] NO_CHILDREN = new SimpleNode[0];

    protected SimpleNode(Project project) {
        this(project, null);
    }

    protected SimpleNode(Project project,  NodeDescriptor parentDescriptor) {
        super(project, parentDescriptor);
        myName = "";
    }

    protected SimpleNode(SimpleNode parent) {
        this(parent == null ? null : parent.myProject, parent);
    }

    public PresentableNodeDescriptor getChildToHighlightAt(int index) {
        return getChildAt(index);
    }

    protected SimpleNode() {
        super(null, null);
    }

    public String toString() {
        return getName();
    }

    public int getWeight() {
        return 10;
    }

    protected SimpleTextAttributes getErrorAttributes() {
        return new SimpleTextAttributes(SimpleTextAttributes.STYLE_WAVED, getColor(), JBColor.RED);
    }

    protected SimpleTextAttributes getPlainAttributes() {
        return new SimpleTextAttributes(Font.PLAIN, getColor());
    }

    private FileStatus getFileStatus() {
        return FileStatus.NOT_CHANGED;
    }

    
    protected Object updateElement() {
        return getElement();
    }

    protected void update(PresentationData presentation) {
        Object newElement = updateElement();
        if (getElement() != newElement) {
            presentation.setChanged(true);
        }
        if (newElement == null) return;

        Color oldColor = myColor;
        String oldName = myName;
        Icon oldIcon = getIcon();
        List<ColoredFragment> oldFragments = new ArrayList<ColoredFragment>(presentation.getColoredText());

        myColor = UIUtil.getTreeTextForeground();
        updateFileStatus();

        doUpdate();

        myName = getName();
        presentation.setPresentableText(myName);

        presentation.setChanged(!Comparing.equal(new Object[]{getIcon(), myName, oldFragments, myColor},
                new Object[]{oldIcon, oldName, oldFragments, oldColor}));

        presentation.setForcedTextForeground(myColor);
        presentation.setIcon(getIcon());
    }

    protected void updateFileStatus() {
        assert getFileStatus() != null : getClass().getName() + ' ' + toString();

        Color fileStatusColor = getFileStatus().getColor();
        if (fileStatusColor != null) {
            myColor = fileStatusColor;
        }
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void setNodeText(String text, String tooltip, boolean hasError) {
        clearColoredText();
        SimpleTextAttributes attributes = hasError ? getErrorAttributes() : getPlainAttributes();
        getTemplatePresentation().addText(new ColoredFragment(text, tooltip, attributes));
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void setPlainText(String aText) {
        clearColoredText();
        addPlainText(aText);
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void addPlainText(String aText) {
        getTemplatePresentation().addText(new ColoredFragment(aText, getPlainAttributes()));
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void addErrorText(String aText, String errorTooltipText) {
        getTemplatePresentation().addText(new ColoredFragment(aText, errorTooltipText, getErrorAttributes()));
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void clearColoredText() {
        getTemplatePresentation().clearText();
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void addColoredFragment(String aText, SimpleTextAttributes aAttributes) {
        addColoredFragment(aText, null, aAttributes);
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void addColoredFragment(String aText, String toolTip, SimpleTextAttributes aAttributes) {
        getTemplatePresentation().addText(new ColoredFragment(aText, toolTip, aAttributes));
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public final void addColoredFragment(ColoredFragment fragment) {
        getTemplatePresentation().addText(new ColoredFragment(fragment.getText(), fragment.getAttributes()));
    }

    protected void doUpdate() {
    }

    public Object getElement() {
        return this;
    }

    public final SimpleNode getParent() {
        return (SimpleNode)getParentDescriptor();
    }

    public int getIndex(SimpleNode child) {
        final SimpleNode[] kids = getChildren();
        for (int i = 0; i < kids.length; i++) {
            SimpleNode each = kids[i];
            if (each.equals(child)) return i;
        }

        return -1;
    }

    public abstract SimpleNode[] getChildren();

    public void accept(SimpleNodeVisitor visitor) {
        visitor.accept(this);
    }

    public void handleSelection(SimpleTree tree) {
    }

    public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
    }

    public boolean isAlwaysShowPlus() {
        return false;
    }

    public boolean isAutoExpandNode() {
        return false;
    }

    public boolean isAlwaysLeaf() {
        return false;
    }

    public boolean shouldHaveSeparator() {
        return false;
    }

    /**
     * @deprecated use {@link #getTemplatePresentation()} to set constant presentation right in node's constructor
     * or update presentation dynamically by defining {@link #update(com.gome.maven.ide.projectView.PresentationData)}
     */
    public void setUniformIcon(Icon aIcon) {
        setIcon(aIcon);
    }

    /**
     * @deprecated never called by Tree classes
     */
    public final ColoredFragment[] getColoredText() {
        final List<ColoredFragment> list = getTemplatePresentation().getColoredText();
        return list.toArray(new ColoredFragment[list.size()]);
    }

    
    public Object[] getEqualityObjects() {
        return NONE;
    }

    public int getChildCount() {
        return getChildren().length;
    }

    public SimpleNode getChildAt(final int i) {
        return getChildren()[i];
    }


    public final boolean equals(Object o) {
        return ComparableObjectCheck.equals(this, o);
    }

    public final int hashCode() {
        return ComparableObjectCheck.hashCode(this, super.hashCode());
    }

}
