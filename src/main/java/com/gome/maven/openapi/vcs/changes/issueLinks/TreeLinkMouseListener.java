/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.vcs.changes.issueLinks;

import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.ui.ColoredTreeCellRenderer;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;

/**
 * @author yole
 */
public class TreeLinkMouseListener extends LinkMouseListenerBase {
    private final ColoredTreeCellRenderer myRenderer;
    protected WeakReference<TreeNode> myLastHitNode;

    public TreeLinkMouseListener(final ColoredTreeCellRenderer renderer) {
        myRenderer = renderer;
    }

    protected void showTooltip(final JTree tree, final MouseEvent e, final HaveTooltip launcher) {
        final String text = tree.getToolTipText(e);
        final String newText = launcher == null ? null : launcher.getTooltip();
        if (!Comparing.equal(text, newText)) {
            tree.setToolTipText(newText);
        }
    }


    @Override
    protected Object getTagAt( final MouseEvent e) {
        JTree tree = (JTree)e.getSource();
        Object tag = null;
        HaveTooltip haveTooltip = null;
        final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path != null) {
            final Rectangle rectangle = tree.getPathBounds(path);
            assert rectangle != null;
            int dx = e.getX() - rectangle.x;
            final TreeNode treeNode = (TreeNode)path.getLastPathComponent();
            if (myLastHitNode == null || myLastHitNode.get() != treeNode) {
                if (doCacheLastNode()) {
                    myLastHitNode = new WeakReference<TreeNode>(treeNode);
                }
                myRenderer.getTreeCellRendererComponent(tree, treeNode, false, false, treeNode.isLeaf(), tree.getRowForPath(path), false);
            }
            tag = myRenderer.getFragmentTagAt(dx);
            if (tag != null && treeNode instanceof HaveTooltip) {
                haveTooltip = (HaveTooltip)treeNode;
            }
        }
        showTooltip(tree, e, haveTooltip);
        return tag;
    }

    protected boolean doCacheLastNode() {
        return true;
    }

    public interface HaveTooltip {
        String getTooltip();
    }
}