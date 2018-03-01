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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.annotation.Annotation;
import com.gome.maven.lang.annotation.AnnotationHolder;
import com.gome.maven.lang.annotation.AnnotationSession;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.SmartList;
import com.gome.maven.xml.util.XmlStringUtil;

/**
 * @author max
 */
public class AnnotationHolderImpl extends SmartList<Annotation> implements AnnotationHolder {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.daemon.impl.AnnotationHolderImpl");
    private final AnnotationSession myAnnotationSession;

    private final boolean myBatchMode;

    public AnnotationHolderImpl( AnnotationSession session) {
        this(session, false);
    }

    public AnnotationHolderImpl( AnnotationSession session, boolean batchMode) {
        myAnnotationSession = session;
        myBatchMode = batchMode;
    }

    @Override
    public boolean isBatchMode() {
        return myBatchMode;
    }

    @Override
    public Annotation createErrorAnnotation( PsiElement elt, String message) {
        assertMyFile(elt);
        return createAnnotation(HighlightSeverity.ERROR, elt.getTextRange(), message);
    }

    @Override
    public Annotation createErrorAnnotation( ASTNode node, String message) {
        assertMyFile(node.getPsi());
        return createAnnotation(HighlightSeverity.ERROR, node.getTextRange(), message);
    }

    @Override
    public Annotation createErrorAnnotation( TextRange range, String message) {
        return createAnnotation(HighlightSeverity.ERROR, range, message);
    }

    @Override
    public Annotation createWarningAnnotation( PsiElement elt, String message) {
        assertMyFile(elt);
        return createAnnotation(HighlightSeverity.WARNING, elt.getTextRange(), message);
    }

    @Override
    public Annotation createWarningAnnotation( ASTNode node, String message) {
        assertMyFile(node.getPsi());
        return createAnnotation(HighlightSeverity.WARNING, node.getTextRange(), message);
    }

    @Override
    public Annotation createWarningAnnotation( TextRange range, String message) {
        return createAnnotation(HighlightSeverity.WARNING, range, message);
    }

    @Override
    public Annotation createWeakWarningAnnotation( PsiElement elt,  String message) {
        assertMyFile(elt);
        return createAnnotation(HighlightSeverity.WEAK_WARNING, elt.getTextRange(), message);
    }

    @Override
    public Annotation createWeakWarningAnnotation( ASTNode node,  String message) {
        assertMyFile(node.getPsi());
        return createAnnotation(HighlightSeverity.WEAK_WARNING, node.getTextRange(), message);
    }

    @Override
    public Annotation createWeakWarningAnnotation( TextRange range, String message) {
        return createAnnotation(HighlightSeverity.WEAK_WARNING, range, message);
    }

    @Override
    public Annotation createInfoAnnotation( PsiElement elt, String message) {
        assertMyFile(elt);
        return createAnnotation(HighlightSeverity.INFORMATION, elt.getTextRange(), message);
    }

    @Override
    public Annotation createInfoAnnotation( ASTNode node, String message) {
        assertMyFile(node.getPsi());
        return createAnnotation(HighlightSeverity.INFORMATION, node.getTextRange(), message);
    }

    private void assertMyFile(PsiElement node) {
        if (node == null) return;
        PsiFile myFile = myAnnotationSession.getFile();
        PsiFile containingFile = node.getContainingFile();
        LOG.assertTrue(containingFile != null, node);
        VirtualFile containingVFile = containingFile.getVirtualFile();
        VirtualFile myVFile = myFile.getVirtualFile();
        if (!Comparing.equal(containingVFile, myVFile)) {
            LOG.error(
                    "Annotation must be registered for an element inside '" + myFile + "' which is in '" + myVFile + "'.\n" +
                            "Element passed: '" + node + "' is inside the '" + containingFile + "' which is in '" + containingVFile + "'");
        }
    }

    @Override
    public Annotation createInfoAnnotation( TextRange range, String message) {
        return createAnnotation(HighlightSeverity.INFORMATION, range, message);
    }

    @Override
    public Annotation createAnnotation( HighlightSeverity severity,  TextRange range,  String message) {
         String tooltip = message == null ? null : XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message));
        return createAnnotation(severity, range, message, tooltip);
    }

    @Override
    public Annotation createAnnotation( HighlightSeverity severity,  TextRange range,  String message,
                                        String tooltip) {
        Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), severity, message, tooltip);
        add(annotation);
        return annotation;
    }

    public boolean hasAnnotations() {
        return !isEmpty();
    }

    
    @Override
    public AnnotationSession getCurrentAnnotationSession() {
        return myAnnotationSession;
    }
}
