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

package com.gome.maven.ide.projectView.impl.nodes;

import com.gome.maven.codeInsight.navigation.NavigationUtil;
import com.gome.maven.ide.projectView.PresentationData;
import com.gome.maven.ide.projectView.ProjectViewNode;
import com.gome.maven.ide.projectView.ProjectViewNodeDecorator;
import com.gome.maven.ide.projectView.ViewSettings;
import com.gome.maven.ide.util.treeView.AbstractTreeNode;
import com.gome.maven.ide.util.treeView.ValidateableNode;
import com.gome.maven.navigation.NavigationItem;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.colors.CodeInsightColors;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.vcs.FileStatus;
import com.gome.maven.openapi.vcs.FileStatusManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.pom.StatePreservingNavigatable;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilBase;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Class for node descriptors based on PsiElements. Subclasses should define
 * method that extract PsiElement from Value.
 * @param <Value> Value of node descriptor
 */
public abstract class AbstractPsiBasedNode<Value> extends ProjectViewNode<Value> implements ValidateableNode, StatePreservingNavigatable {
    private static final Logger LOG = Logger.getInstance(AbstractPsiBasedNode.class.getName());

    protected AbstractPsiBasedNode(final Project project,
                                   final Value value,
                                   final ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    
    protected abstract PsiElement extractPsiFromValue();
    
    protected abstract Collection<AbstractTreeNode> getChildrenImpl();
    protected abstract void updateImpl(final PresentationData data);

    @Override
    
    public final Collection<AbstractTreeNode> getChildren() {
        final PsiElement psiElement = extractPsiFromValue();
        if (psiElement == null) {
            return new ArrayList<AbstractTreeNode>();
        }
        final boolean valid = psiElement.isValid();
        if (!LOG.assertTrue(valid)) {
            return Collections.emptyList();
        }

        final Collection<AbstractTreeNode> children = getChildrenImpl();
        return children != null ? children : Collections.<AbstractTreeNode>emptyList();
    }

    @Override
    public boolean isValid() {
        final PsiElement psiElement = extractPsiFromValue();
        return psiElement != null && psiElement.isValid();
    }

    protected boolean isMarkReadOnly() {
        final AbstractTreeNode parent = getParent();
        if (parent == null) {
            return false;
        }
        if (parent instanceof AbstractPsiBasedNode) {
            final PsiElement psiElement = ((AbstractPsiBasedNode)parent).extractPsiFromValue();
            return psiElement instanceof PsiDirectory;
        }

        final Object parentValue = parent.getValue();
        return parentValue instanceof PsiDirectory || parentValue instanceof Module;
    }


    @Override
    public FileStatus getFileStatus() {
        VirtualFile file = getVirtualFileForValue();
        if (file == null) {
            return FileStatus.NOT_CHANGED;
        }
        else {
            return FileStatusManager.getInstance(getProject()).getStatus(file);
        }
    }

    
    private VirtualFile getVirtualFileForValue() {
        PsiElement psiElement = extractPsiFromValue();
        if (psiElement == null) {
            return null;
        }
        return PsiUtilBase.getVirtualFile(psiElement);
    }

    // Should be called in atomic action

    @Override
    public void update(final PresentationData data) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                if (!validate()) {
                    return;
                }

                final PsiElement value = extractPsiFromValue();
                LOG.assertTrue(value.isValid());

                int flags = getIconableFlags();

                try {
                    Icon icon = value.getIcon(flags);
                    data.setIcon(icon);
                }
                catch (IndexNotReadyException ignored) {
                }
                data.setPresentableText(myName);

                try {
                    if (isDeprecated()) {
                        data.setAttributesKey(CodeInsightColors.DEPRECATED_ATTRIBUTES);
                    }
                }
                catch (IndexNotReadyException ignored) {
                }
                updateImpl(data);
                for (ProjectViewNodeDecorator decorator : Extensions.getExtensions(ProjectViewNodeDecorator.EP_NAME, myProject)) {
                    decorator.decorate(AbstractPsiBasedNode.this, data);
                }

                Iconable.LastComputedIcon.put(value, data.getIcon(false), flags);
            }
        });
    }

    @Iconable.IconFlags
    protected int getIconableFlags() {
        int flags = Iconable.ICON_FLAG_VISIBILITY;
        if (isMarkReadOnly()) {
            flags |= Iconable.ICON_FLAG_READ_STATUS;
        }
        return flags;
    }

    protected boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean contains( final VirtualFile file) {
        final PsiElement psiElement = extractPsiFromValue();
        if (psiElement == null || !psiElement.isValid()) {
            return false;
        }

        final PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile == null) {
            return false;
        }
        final VirtualFile valueFile = containingFile.getVirtualFile();
        return valueFile != null && file.equals(valueFile);
    }

    
    public NavigationItem getNavigationItem() {
        final PsiElement psiElement = extractPsiFromValue();
        return (psiElement instanceof NavigationItem) ? (NavigationItem) psiElement : null;
    }

    @Override
    public void navigate(boolean requestFocus, boolean preserveState) {
        if (canNavigate()) {
            if (requestFocus || preserveState) {
                NavigationUtil.openFileWithPsiElement(extractPsiFromValue(), requestFocus, requestFocus);
            }
            else {
                getNavigationItem().navigate(requestFocus);
            }
        }
    }

    @Override
    public void navigate(boolean requestFocus) {
        navigate(requestFocus, false);
    }

    @Override
    public boolean canNavigate() {
        final NavigationItem item = getNavigationItem();
        return item != null && item.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        final NavigationItem item = getNavigationItem();
        return item != null && item.canNavigateToSource();
    }

    
    protected String calcTooltip() {
        return null;
    }

    @Override
    public boolean validate() {
        final PsiElement psiElement = extractPsiFromValue();
        if (psiElement == null || !psiElement.isValid()) {
            setValue(null);
        }

        return getValue() != null;
    }
}
