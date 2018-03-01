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
package com.gome.maven.openapi.vcs.changes.ui;

import com.gome.maven.ide.CopyProvider;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.ObjectUtils;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.tree.TreeUtil;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;

class ChangesBrowserNodeCopyProvider implements CopyProvider {

     private final JTree myTree;

    ChangesBrowserNodeCopyProvider( JTree tree) {
        myTree = tree;
    }

    public boolean isCopyEnabled( DataContext dataContext) {
        return myTree.getSelectionPaths() != null;
    }

    public boolean isCopyVisible( DataContext dataContext) {
        return true;
    }

    public void performCopy( DataContext dataContext) {
        List<TreePath> paths = ContainerUtil.sorted(Arrays.asList(ObjectUtils.assertNotNull(myTree.getSelectionPaths())),
                TreeUtil.getDisplayOrderComparator(myTree));
        CopyPasteManager.getInstance().setContents(new StringSelection(StringUtil.join(paths, new Function<TreePath, String>() {
            @Override
            public String fun(TreePath path) {
                Object node = path.getLastPathComponent();
                if (node instanceof ChangesBrowserNode) {
                    return ((ChangesBrowserNode)node).getTextPresentation();
                }
                else {
                    return node.toString();
                }
            }
        }, "\n")));
    }
}
