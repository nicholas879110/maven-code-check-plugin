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
package com.gome.maven.psi.impl.source.xml.behavior;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiFileFactory;
import com.gome.maven.psi.impl.source.DummyHolderFactory;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.psi.impl.source.tree.SharedImplUtil;
import com.gome.maven.psi.impl.source.tree.TreeElement;
import com.gome.maven.psi.impl.source.tree.TreeUtil;
import com.gome.maven.psi.impl.source.xml.XmlPsiPolicy;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.psi.xml.XmlTagChild;
import com.gome.maven.util.CharTable;

public class DefaultXmlPsiPolicy implements XmlPsiPolicy{
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.xml.behavior.DefaultXmlPsiPolicy");

    @Override
    public ASTNode encodeXmlTextContents(String displayText, PsiElement text) {
        final PsiFile containingFile = text.getContainingFile();
        CharTable charTable = SharedImplUtil.findCharTableByTree(text.getNode());
        final FileElement dummyParent = DummyHolderFactory.createHolder(text.getManager(), null, charTable).getTreeElement();
        final XmlTag rootTag =
                ((XmlFile)PsiFileFactory.getInstance(containingFile.getProject())
                        .createFileFromText("a.xml", "<a>" + displayText + "</a>")).getRootTag();

        assert rootTag != null;
        final XmlTagChild[] tagChildren = rootTag.getValue().getChildren();

        final XmlTagChild child = tagChildren.length > 0 ? tagChildren[0]:null;
        LOG.assertTrue(child != null, "Child is null for tag: " + rootTag.getText());

        final TreeElement element = (TreeElement)child.getNode();
        ((TreeElement)tagChildren[tagChildren.length - 1].getNode().getTreeNext()).rawRemoveUpToLast();
        dummyParent.rawAddChildren(element);
        TreeUtil.clearCaches(dummyParent);
        return element.getFirstChildNode();
    }

}
