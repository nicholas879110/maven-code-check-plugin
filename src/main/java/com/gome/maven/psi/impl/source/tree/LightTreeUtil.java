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
package com.gome.maven.psi.impl.source.tree;

import com.gome.maven.lang.LighterAST;
import com.gome.maven.lang.LighterASTNode;
import com.gome.maven.lang.LighterASTTokenNode;
import com.gome.maven.lang.LighterLazyParseableNode;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.SmartList;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class LightTreeUtil {
    private LightTreeUtil() { }

    
    public static LighterASTNode firstChildOfType( LighterAST tree,  LighterASTNode node,  IElementType type) {
        List<LighterASTNode> children = tree.getChildren(node);
        for (int i = 0, size = children.size(); i < size; ++i) {
            LighterASTNode child = children.get(i);
            if (child.getTokenType() == type) return child;
        }

        return null;
    }

    
    public static LighterASTNode firstChildOfType( LighterAST tree,  LighterASTNode node,  TokenSet types) {
        List<LighterASTNode> children = tree.getChildren(node);
        for (int i = 0, size = children.size(); i < size; ++i) {
            LighterASTNode child = children.get(i);
            if (types.contains(child.getTokenType())) return child;
        }

        return null;
    }

    
    public static LighterASTNode requiredChildOfType( LighterAST tree,  LighterASTNode node,  IElementType type) {
        LighterASTNode child = firstChildOfType(tree, node, type);
        assert child != null : "Required child " + type + " not found in " + node.getTokenType() + ": " + tree.getChildren(node);
        return child;
    }

    
    public static LighterASTNode requiredChildOfType( LighterAST tree,  LighterASTNode node,  TokenSet types) {
        LighterASTNode child = firstChildOfType(tree, node, types);
        assert child != null : "Required child " + types + " not found in " + node.getTokenType() + ": " + tree.getChildren(node);
        return child;
    }

    
    public static List<LighterASTNode> getChildrenOfType( LighterAST tree,  LighterASTNode node,  IElementType type) {
        List<LighterASTNode> result = null;

        List<LighterASTNode> children = tree.getChildren(node);
        for (int i = 0, size = children.size(); i < size; ++i) {
            LighterASTNode child = children.get(i);
            if (child.getTokenType() == type) {
                if (result == null) result = new SmartList<LighterASTNode>();
                result.add(child);
            }
        }

        return result != null ? result: Collections.<LighterASTNode>emptyList();
    }

    
    public static List<LighterASTNode> getChildrenOfType( LighterAST tree,  LighterASTNode node,  TokenSet types) {
        List<LighterASTNode> result = null;

        List<LighterASTNode> children = tree.getChildren(node);
        for (int i = 0, size = children.size(); i < size; ++i) {
            LighterASTNode child = children.get(i);
            if (types.contains(child.getTokenType())) {
                if (result == null) result = new SmartList<LighterASTNode>();
                result.add(child);
            }
        }

        return result != null ? result: Collections.<LighterASTNode>emptyList();
    }

    
    public static String toFilteredString( LighterAST tree,  LighterASTNode node,  TokenSet skipTypes) {
        int length = node.getEndOffset() - node.getStartOffset();
        if (length < 0) {
            length = 0;
            Logger.getInstance(LightTreeUtil.class).error("tree=" + tree + " node=" + node);
        }
        StringBuilder buffer = new StringBuilder(length);
        toBuffer(tree, node, buffer, skipTypes);
        return buffer.toString();
    }

    public static void toBuffer( LighterAST tree,  LighterASTNode node,  StringBuilder buffer,  TokenSet skipTypes) {
        if (skipTypes != null && skipTypes.contains(node.getTokenType())) return;

        if (node instanceof LighterASTTokenNode) {
            buffer.append(((LighterASTTokenNode)node).getText());
            return;
        }

        if (node instanceof LighterLazyParseableNode) {
            buffer.append(((LighterLazyParseableNode)node).getText());
            return;
        }

        List<LighterASTNode> children = tree.getChildren(node);
        for (int i = 0, size = children.size(); i < size; ++i) {
            toBuffer(tree, children.get(i), buffer, skipTypes);
        }
    }
}
