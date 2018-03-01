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
package com.gome.maven.psi.impl.source.xml;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.NullableComputable;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.xml.XmlElementDescriptor;
import com.gome.maven.xml.actions.validate.ValidateXmlActionHandler;
import com.gome.maven.xml.actions.validate.ErrorReporter;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.SubstitutionGroupHandler;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSGrammarBucket;
import org.apache.xerces.impl.xs.models.CMBuilder;
import org.apache.xerces.impl.xs.models.CMNodeFactory;
import org.apache.xerces.impl.xs.models.XSCMValidator;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
class XsContentDFA extends XmlContentDFA {

    private final XSCMValidator myContentModel;
    private final SubstitutionGroupHandler myHandler;
    private final int[] myState;
    private final XmlElementDescriptor[] myElementDescriptors;

    
    public static XmlContentDFA createContentDFA( XmlTag parentTag) {
        final PsiFile file = parentTag.getContainingFile().getOriginalFile();
        if (!(file instanceof XmlFile)) return null;
        XSModel xsModel = ApplicationManager.getApplication().runReadAction(new NullableComputable<XSModel>() {
            @Override
            public XSModel compute() {
                return getXSModel((XmlFile)file);
            }
        });
        if (xsModel == null) {
            return null;
        }

        XSElementDeclaration decl = getElementDeclaration(parentTag, xsModel);
        if (decl == null) {
            return null;
        }
        return new XsContentDFA(decl, parentTag);
    }

    public XsContentDFA( XSElementDeclaration decl, final XmlTag parentTag) {
        XSComplexTypeDecl definition = (XSComplexTypeDecl)decl.getTypeDefinition();
        myContentModel = definition.getContentModel(new CMBuilder(new CMNodeFactory()));
        myHandler = new SubstitutionGroupHandler(new XSGrammarBucket());
        myState = myContentModel.startContentModel();
        myElementDescriptors = ApplicationManager.getApplication().runReadAction(new Computable<XmlElementDescriptor[]>() {

            @Override
            public XmlElementDescriptor[] compute() {
                XmlElementDescriptor parentTagDescriptor = parentTag.getDescriptor();
                assert parentTagDescriptor != null;
                return parentTagDescriptor.getElementsDescriptors(parentTag);
            }
        });
    }

    @Override
    public List<XmlElementDescriptor> getPossibleElements() {
        final List vector = myContentModel.whatCanGoHere(myState);
        ArrayList<XmlElementDescriptor> list = new ArrayList<XmlElementDescriptor>();
        for (Object o : vector) {
            if (o instanceof XSElementDecl) {
                final XSElementDecl elementDecl = (XSElementDecl)o;
                XmlElementDescriptor descriptor = ContainerUtil.find(myElementDescriptors, new Condition<XmlElementDescriptor>() {
                    @Override
                    public boolean value(XmlElementDescriptor elementDescriptor) {
                        return elementDecl.getName().equals(elementDescriptor.getName());
                    }
                });
                ContainerUtil.addIfNotNull(descriptor, list);
            }
        }
        return list;
    }

    @Override
    public void transition(XmlTag xmlTag) {
        myContentModel.oneTransition(createQName(xmlTag), myState, myHandler);
    }

    private static QName createQName(XmlTag tag) {
        //todo don't use intern to not pollute PermGen
        String namespace = tag.getNamespace();
        return new QName(tag.getNamespacePrefix().intern(),
                tag.getLocalName().intern(),
                tag.getName().intern(),
                namespace.isEmpty() ? null : namespace.intern());
    }

    
    private static XSElementDeclaration getElementDeclaration(XmlTag tag, XSModel xsModel) {

        List<XmlTag> ancestors = new ArrayList<XmlTag>();
        for (XmlTag t = tag; t != null; t = t.getParentTag()) {
            ancestors.add(t);
        }
        Collections.reverse(ancestors);
        XSElementDeclaration declaration = null;
        SubstitutionGroupHandler fSubGroupHandler = new SubstitutionGroupHandler(new XSGrammarBucket());
        CMBuilder cmBuilder = new CMBuilder(new CMNodeFactory());
        for (XmlTag ancestor : ancestors) {
            if (declaration == null) {
                declaration = xsModel.getElementDeclaration(ancestor.getLocalName(), ancestor.getNamespace());
                if (declaration == null) return null;
                else continue;
            }
            XSTypeDefinition typeDefinition = declaration.getTypeDefinition();
            if (!(typeDefinition instanceof XSComplexTypeDecl)) {
                return null;
            }

            XSCMValidator model = ((XSComplexTypeDecl)typeDefinition).getContentModel(cmBuilder);
            int[] ints = model.startContentModel();
            for (XmlTag subTag : ancestor.getParentTag().getSubTags()) {
                QName qName = createQName(subTag);
                Object o = model.oneTransition(qName, ints, fSubGroupHandler);
                if (subTag == ancestor) {
                    if (o instanceof XSElementDecl) {
                        declaration = (XSElementDecl)o;
                        break;
                    }
                    else return null;
                }
            }
        }
        return declaration;
    }

    
    private static XSModel getXSModel(XmlFile file) {

        ValidateXmlActionHandler handler = new ValidateXmlActionHandler(false) {
            @Override
            protected SAXParser createParser() throws SAXException, ParserConfigurationException {
                SAXParser parser = super.createParser();
                parser.getXMLReader().setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE, true);
                return parser;
            }
        };
        handler.setErrorReporter(new ErrorReporter(handler) {

            int count;
            @Override
            public void processError(SAXParseException ex, ValidateXmlActionHandler.ProblemType warning) throws SAXException {
                if (warning != ValidateXmlActionHandler.ProblemType.WARNING && count++ > 100) {
                    throw new SAXException(ex);
                }
            }

            @Override
            public boolean isUniqueProblem(SAXParseException e) {
                return true;
            }
        });

        handler.doValidate(file);
        XMLGrammarPool grammarPool = ValidateXmlActionHandler.getGrammarPool(file);
        if (grammarPool == null) {
            return null;
        }
        Grammar[] grammars = grammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);

        return grammars.length == 0 ? null : ((XSGrammar)grammars[0]).toXSModel(ContainerUtil.map(grammars, new Function<Grammar, XSGrammar>() {
            @Override
            public XSGrammar fun(Grammar grammar) {
                return (XSGrammar)grammar;
            }
        }, new XSGrammar[0]));
    }

}