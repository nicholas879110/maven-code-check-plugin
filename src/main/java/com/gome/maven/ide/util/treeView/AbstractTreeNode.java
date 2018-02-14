/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.ide.util.treeView;

import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.editor.colors.CodeInsightColors;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Queryable;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.awt.*;
import java.util.Collection;
import java.util.Map;

public abstract class AbstractTreeNode<T> extends PresentableNodeDescriptor<AbstractTreeNode<T>> implements NavigationItem, Queryable.Contributor {
    private AbstractTreeNode myParent;
    private Object myValue;
    private NodeDescriptor myParentDescriptor;

    protected AbstractTreeNode(Project project, T value) {
        super(project, null);
        setValue(value);
    }


    public abstract Collection<? extends AbstractTreeNode> getChildren();


    protected boolean hasProblemFileBeneath() {
        return false;
    }

    protected boolean valueIsCut() {
        return CopyPasteManager.getInstance().isCutElement(getValue());
    }

    @Override
    public PresentableNodeDescriptor getChildToHighlightAt(int index) {
        final Collection<? extends AbstractTreeNode> kids = getChildren();
        int i = 0;
        for (final AbstractTreeNode kid : kids) {
            if (i == index) return kid;
            i++;
        }

        return null;
    }

    @Override
    protected void postprocess(PresentationData presentation) {
        if (hasProblemFileBeneath()) {
            presentation.setAttributesKey(CodeInsightColors.ERRORS_ATTRIBUTES);
        }

        setForcedForeground(presentation);

        if (hasProblemFileBeneath()) {
            presentation.setAttributesKey(CodeInsightColors.ERRORS_ATTRIBUTES);
        }
    }

    protected void setForcedForeground(PresentationData presentation) {
        final FileStatus status = getFileStatus();
        Color fgColor = getFileStatusColor(status);
        fgColor = fgColor == null ? status.getColor() : fgColor;

        if (valueIsCut()) {
            fgColor = CopyPasteManager.CUT_COLOR;
        }

        if (presentation.getForcedTextForeground() == null) {
            presentation.setForcedTextForeground(fgColor);
        }
    }

    @Override
    protected boolean shouldUpdateData() {
        return !myProject.isDisposed() && myValue != null;
    }

    public boolean isAlwaysShowPlus() {
        return false;
    }

    public boolean isAlwaysLeaf() {
        return false;
    }

    public boolean isAlwaysExpand() {
        return false;
    }

    @Override

    public final AbstractTreeNode<T> getElement() {
        return myValue != null ? this : null;
    }

    public boolean equals(Object object) {
        return object instanceof AbstractTreeNode && Comparing.equal(myValue, ((AbstractTreeNode) object).myValue);
    }

    public int hashCode() {
        Object value = myValue;
        return value == null ? 0 : value.hashCode();
    }

    public final AbstractTreeNode getParent() {
        return myParent;
    }

    public final void setParent(AbstractTreeNode parent) {
        myParent = parent;
        myParentDescriptor = parent;
    }

    @Override
    public final NodeDescriptor getParentDescriptor() {
        return myParentDescriptor;
    }

    public final T getValue() {
        if (myValue == null) {
            return null;
        }
        return (T) TreeAnchorizer.getService().retrieveElement(myValue);
    }

    public final void setValue(T value) {
        myValue = value == null ? null : TreeAnchorizer.getService().createAnchor(value);
    }

    public final Object getEqualityObject() {
        return myValue;
    }


    public String toTestString(Queryable.PrintInfo printInfo) {
        if (getValue() instanceof Queryable) {
            String text = Queryable.Util.print((Queryable) getValue(), printInfo, this);
            if (text != null) return text;
        }

        return getTestPresentation();
    }

    @Override
    public void apply(Map<String, String> info) {
    }

    /**
     * @return
     * @deprecated use toTestString
     */

    public String getTestPresentation() {
        if (myName != null) {
            return myName;
        }
        if (getValue() != null) {
            return getValue().toString();
        }
        return null;
    }

    public Color getFileStatusColor(final FileStatus status) {
        if (FileStatus.NOT_CHANGED.equals(status)) {
            final VirtualFile vf = getVirtualFile();
            if (vf != null && vf.isDirectory()) {
                return FileStatusManager.getInstance(myProject).getNotChangedDirectoryColor(vf);
            }
        }
        return status.getColor();
    }

    protected VirtualFile getVirtualFile() {
        return null;
    }

    public FileStatus getFileStatus() {
        return FileStatus.NOT_CHANGED;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public void navigate(boolean requestFocus) {
    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }


    protected final Object getParentValue() {
        AbstractTreeNode parent = getParent();
        return parent == null ? null : parent.getValue();
    }


    public boolean canRepresent(final Object element) {
        return Comparing.equal(getValue(), element);
    }

    /**
     * @deprecated use {@link #getPresentation()} instead
     */
    protected String getToolTip() {
        return getPresentation().getTooltip();
    }

    /**
     * @deprecated use {@link #getPresentation()} instead
     */

    public TextAttributesKey getAttributesKey() {
        return getPresentation().getTextAttributesKey();
    }

    /**
     * @deprecated use {@link #getPresentation()} instead
     */

    public String getLocationString() {
        return getPresentation().getLocationString();
    }


}
