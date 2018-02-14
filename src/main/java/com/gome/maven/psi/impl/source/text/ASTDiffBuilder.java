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

/*
 * @author max
 */
package com.gome.maven.psi.impl.source.text;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.pom.PomManager;
import com.gome.maven.pom.tree.TreeAspect;
import com.gome.maven.pom.tree.events.ChangeInfo;
import com.gome.maven.pom.tree.events.impl.ChangeInfoImpl;
import com.gome.maven.pom.tree.events.impl.ReplaceChangeInfoImpl;
import com.gome.maven.pom.tree.events.impl.TreeChangeEventImpl;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.util.diff.DiffTreeChangeBuilder;

public class ASTDiffBuilder implements DiffTreeChangeBuilder<ASTNode, ASTNode> {
    private final TreeChangeEventImpl myEvent;

    public ASTDiffBuilder( PsiFileImpl fileImpl) {
        TreeAspect modelAspect = PomManager.getModel(fileImpl.getProject()).getModelAspect(TreeAspect.class);
        myEvent = new TreeChangeEventImpl(modelAspect, fileImpl.getTreeElement());
    }

    @Override
    public void nodeReplaced( ASTNode oldNode,  ASTNode newNode) {
        if (oldNode instanceof FileElement && newNode instanceof FileElement) {
        }
        else {
            final ReplaceChangeInfoImpl change = new ReplaceChangeInfoImpl(newNode);
            change.setReplaced(oldNode);

            myEvent.addElementaryChange(newNode, change);
        }
    }

    @Override
    public void nodeDeleted( ASTNode parent,  final ASTNode child) {
        myEvent.addElementaryChange(child, ChangeInfoImpl.create(ChangeInfo.REMOVED, child));
    }

    @Override
    public void nodeInserted( final ASTNode oldParent,  ASTNode newNode, final int pos) {
        myEvent.addElementaryChange(newNode, ChangeInfoImpl.create(ChangeInfo.ADD, newNode));
    }

    
    public TreeChangeEventImpl getEvent() {
        return myEvent;
    }
}
