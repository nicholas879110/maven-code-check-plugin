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
package com.gome.maven.lang.impl;

import com.gome.maven.lang.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.tree.IElementType;
import com.gome.maven.psi.tree.TokenSet;
import com.gome.maven.util.diff.FlyweightCapableTreeStructure;

public class PsiBuilderAdapter implements PsiBuilder {
    protected final PsiBuilder myDelegate;

    public PsiBuilderAdapter(final PsiBuilder delegate) {
        myDelegate = delegate;
    }

    public PsiBuilder getDelegate() {
        return myDelegate;
    }

    @Override
    public Project getProject() {
        return myDelegate.getProject();
    }

    @Override
    public CharSequence getOriginalText() {
        return myDelegate.getOriginalText();
    }

    @Override
    public void advanceLexer() {
        myDelegate.advanceLexer();
    }

    @Override 
    public IElementType getTokenType() {
        return myDelegate.getTokenType();
    }

    @Override
    public void setTokenTypeRemapper(final ITokenTypeRemapper remapper) {
        myDelegate.setTokenTypeRemapper(remapper);
    }

    @Override
    public void setWhitespaceSkippedCallback( final WhitespaceSkippedCallback callback) {
        myDelegate.setWhitespaceSkippedCallback(callback);
    }

    @Override
    public void remapCurrentToken(IElementType type) {
        myDelegate.remapCurrentToken(type);
    }

    @Override
    public IElementType lookAhead(int steps) {
        return myDelegate.lookAhead(steps);
    }

    @Override
    public IElementType rawLookup(int steps) {
        return myDelegate.rawLookup(steps);
    }

    @Override
    public int rawTokenTypeStart(int steps) {
        return myDelegate.rawTokenTypeStart(steps);
    }

    @Override
    public int rawTokenIndex() {
        return myDelegate.rawTokenIndex();
    }

    @Override
    public String getTokenText() {
        return myDelegate.getTokenText();
    }

    @Override
    public int getCurrentOffset() {
        return myDelegate.getCurrentOffset();
    }

    @Override
    public Marker mark() {
        return myDelegate.mark();
    }

    @Override
    public void error(final String messageText) {
        myDelegate.error(messageText);
    }

    @Override
    public boolean eof() {
        return myDelegate.eof();
    }

    @Override
    public ASTNode getTreeBuilt() {
        return myDelegate.getTreeBuilt();
    }

    @Override
    public FlyweightCapableTreeStructure<LighterASTNode> getLightTree() {
        return myDelegate.getLightTree();
    }

    @Override
    public void setDebugMode(final boolean dbgMode) {
        myDelegate.setDebugMode(dbgMode);
    }

    @Override
    public void enforceCommentTokens( final TokenSet tokens) {
        myDelegate.enforceCommentTokens(tokens);
    }

    @Override 
    public LighterASTNode getLatestDoneMarker() {
        return myDelegate.getLatestDoneMarker();
    }

    @Override 
    public <T> T getUserData( final Key<T> key) {
        return myDelegate.getUserData(key);
    }

    @Override
    public <T> void putUserData( final Key<T> key,  final T value) {
        myDelegate.putUserData(key, value);
    }

    @Override
    public <T> T getUserDataUnprotected( final Key<T> key) {
        return myDelegate.getUserDataUnprotected(key);
    }

    @Override
    public <T> void putUserDataUnprotected( final Key<T> key,  final T value) {
        myDelegate.putUserDataUnprotected(key, value);
    }
}
