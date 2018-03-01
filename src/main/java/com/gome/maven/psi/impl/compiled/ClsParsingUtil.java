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
package com.gome.maven.psi.impl.compiled;

import com.gome.maven.lang.PsiBuilder;
import com.gome.maven.lang.java.lexer.JavaLexer;
import com.gome.maven.lang.java.parser.JavaParser;
import com.gome.maven.lang.java.parser.JavaParserUtil;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.pom.java.LanguageLevel;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.source.DummyHolder;
import com.gome.maven.psi.impl.source.DummyHolderFactory;
import com.gome.maven.psi.impl.source.JavaDummyElement;
import com.gome.maven.psi.impl.source.SourceTreeToPsiMap;
import com.gome.maven.psi.util.PsiUtil;
import com.gome.maven.util.IncorrectOperationException;
import org.jetbrains.org.objectweb.asm.Opcodes;

/**
 * @author ven
 */
public class ClsParsingUtil {
    private static final Logger LOG = Logger.getInstance("com.gome.maven.psi.impl.compiled.ClsParsingUtil");

    private static final JavaParserUtil.ParserWrapper ANNOTATION_VALUE = new JavaParserUtil.ParserWrapper() {
        @Override
        public void parse(final PsiBuilder builder) {
            JavaParser.INSTANCE.getDeclarationParser().parseAnnotationValue(builder);
        }
    };

    private ClsParsingUtil() { }

    public static PsiExpression createExpressionFromText( String exprText,
                                                          PsiManager manager,
                                                          ClsElementImpl parent) {
        PsiJavaParserFacade parserFacade = JavaPsiFacade.getInstance(manager.getProject()).getParserFacade();
        try {
            PsiExpression expr = parserFacade.createExpressionFromText(exprText, null);
            return psiToClsExpression(expr, parent);
        }
        catch (IncorrectOperationException e) {
            LOG.error(e);
            return null;
        }
    }

    public static PsiAnnotationMemberValue createMemberValueFromText( String text,
                                                                      PsiManager manager,
                                                                      ClsElementImpl parent) {
        LanguageLevel level = PsiUtil.getLanguageLevel(parent);
        DummyHolder holder = DummyHolderFactory.createHolder(manager, new JavaDummyElement(text, ANNOTATION_VALUE, level), null);
        PsiElement element = SourceTreeToPsiMap.treeElementToPsi(holder.getTreeElement().getFirstChildNode());
        if (!(element instanceof PsiAnnotationMemberValue)) {
            LOG.error("Could not parse initializer:'" + text + "'");
            return null;
        }
        return getMemberValue(element, parent);
    }

    public static PsiAnnotationMemberValue getMemberValue( PsiElement element,  ClsElementImpl parent) {
        if (element instanceof PsiExpression) {
            return psiToClsExpression((PsiExpression)element, parent);
        }
        else if (element instanceof PsiArrayInitializerMemberValue) {
            PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue)element).getInitializers();
            PsiAnnotationMemberValue[] clsInitializers = new PsiAnnotationMemberValue[initializers.length];
            ClsArrayInitializerMemberValueImpl arrayValue = new ClsArrayInitializerMemberValueImpl(parent, clsInitializers);
            for (int i = 0; i < initializers.length; i++) {
                clsInitializers[i] = getMemberValue(initializers[i], arrayValue);
            }
            return arrayValue;
        }
        else if (element instanceof PsiAnnotation) {
            final PsiAnnotation psiAnnotation = (PsiAnnotation)element;
            final PsiJavaCodeReferenceElement referenceElement = psiAnnotation.getNameReferenceElement();
            assert referenceElement != null : psiAnnotation;
            final String canonicalText = referenceElement.getText(); // class file has FQNs
            return new ClsAnnotationValueImpl(parent) {
                @Override
                protected ClsJavaCodeReferenceElementImpl createReference() {
                    return new ClsJavaCodeReferenceElementImpl(this, canonicalText);
                }

                @Override
                protected ClsAnnotationParameterListImpl createParameterList() {
                    PsiNameValuePair[] psiAttributes = psiAnnotation.getParameterList().getAttributes();
                    return new ClsAnnotationParameterListImpl(this, psiAttributes);
                }

                @Override
                public PsiAnnotationOwner getOwner() {
                    return (PsiAnnotationOwner)getParent();
                }
            };
        }
        else {
            LOG.error("Unexpected source element for annotation member value: " + element);
            return null;
        }
    }

    private static PsiExpression psiToClsExpression(PsiExpression expr,  ClsElementImpl parent) {
        if (expr instanceof PsiLiteralExpression) {
            if (parent != null && ((ClsFileImpl)parent.getContainingFile()).isForDecompiling()) {
                return new ClsLiteralExpressionImpl(parent, expr.getText(), PsiType.NULL, null);
            }
            else {
                return new ClsLiteralExpressionImpl(parent, expr.getText(), expr.getType(), ((PsiLiteralExpression)expr).getValue());
            }
        }
        if (expr instanceof PsiPrefixExpression) {
            PsiPrefixExpression prefixExpr = (PsiPrefixExpression)expr;
            ClsJavaTokenImpl operation = new ClsJavaTokenImpl(null, prefixExpr.getOperationTokenType(), prefixExpr.getOperationSign().getText());
            ClsLiteralExpressionImpl literal = (ClsLiteralExpressionImpl)psiToClsExpression(prefixExpr.getOperand(), null);
            return new ClsPrefixExpressionImpl(parent, operation, literal);
        }
        if (expr instanceof PsiClassObjectAccessExpression) {
            String exprText = expr.getText();
            if (StringUtil.endsWith(exprText, ".class")) {
                String classText = exprText.substring(0, exprText.length() - 6);
                return new ClsClassObjectAccessExpressionImpl(parent, classText);
            }
        }
        if (expr instanceof PsiReferenceExpression) {
            return new ClsReferenceExpressionImpl(parent, (PsiReferenceExpression)expr);
        }
        if (expr instanceof PsiBinaryExpression) {
            PsiBinaryExpression binaryExpr = (PsiBinaryExpression)expr;
            PsiExpression lOperand = psiToClsExpression(binaryExpr.getLOperand(), null);
            ClsJavaTokenImpl operation = new ClsJavaTokenImpl(null, binaryExpr.getOperationTokenType(), binaryExpr.getOperationSign().getText());
            PsiExpression rOperand = psiToClsExpression(binaryExpr.getROperand(), null);
            if (lOperand instanceof ClsLiteralExpressionImpl) {
                return new ClsBinaryExpressionImpl(parent, (ClsLiteralExpressionImpl)lOperand, operation, (ClsLiteralExpressionImpl)rOperand);
            }
            else if (lOperand instanceof ClsPrefixExpressionImpl) {
                return new ClsBinaryExpressionImpl(parent, (ClsPrefixExpressionImpl)lOperand, operation, (ClsLiteralExpressionImpl)rOperand);
            }
        }
        else {
            if (parent != null && ((ClsFileImpl)parent.getContainingFile()).isForDecompiling()) {
                return new ClsLiteralExpressionImpl(parent, expr.getText(), PsiType.NULL, null);
            }
            final PsiConstantEvaluationHelper evaluator = JavaPsiFacade.getInstance(expr.getProject()).getConstantEvaluationHelper();
            final Object value = evaluator.computeConstantExpression(expr);
            if (value != null) {
                return new ClsLiteralExpressionImpl(parent, expr.getText(), expr.getType(), value);
            }
        }

        LOG.error("Unable to compute expression value: " + expr + " [" + expr.getText() + "]");
        return null;
    }

    public static boolean isJavaIdentifier( String identifier,  LanguageLevel level) {
        return StringUtil.isJavaIdentifier(identifier) && !JavaLexer.isKeyword(identifier, level);
    }

    public static LanguageLevel getLanguageLevelByVersion(int major) {
        switch (major) {
            case Opcodes.V1_1:
            case 45:  // other variant of 1.1
            case Opcodes.V1_2:
            case Opcodes.V1_3:
                return LanguageLevel.JDK_1_3;

            case Opcodes.V1_4:
                return LanguageLevel.JDK_1_4;

            case Opcodes.V1_5:
                return LanguageLevel.JDK_1_5;

            case Opcodes.V1_6:
                return LanguageLevel.JDK_1_6;

            case Opcodes.V1_7:
                return LanguageLevel.JDK_1_7;

            case Opcodes.V1_8:
                return LanguageLevel.JDK_1_8;

            default:
                return LanguageLevel.HIGHEST;
        }
    }
}
