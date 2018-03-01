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
package com.gome.maven.psi.impl.java.stubs;

import com.gome.maven.lang.ASTNode;
import com.gome.maven.psi.impl.source.JavaFileElementType;
import com.gome.maven.psi.impl.source.tree.JavaElementType;
import com.gome.maven.psi.impl.source.tree.java.*;
import com.gome.maven.psi.tree.IStubFileElementType;

/**
 * @author max
 */
public interface JavaStubElementTypes {
    JavaModifierListElementType MODIFIER_LIST = new JavaModifierListElementType();
    JavaAnnotationElementType ANNOTATION = new JavaAnnotationElementType();
    JavaAnnotationParameterListType ANNOTATION_PARAMETER_LIST = new JavaAnnotationParameterListType();
    JavaNameValuePairType NAME_VALUE_PAIR = new JavaNameValuePairType();
    JavaParameterListElementType PARAMETER_LIST = new JavaParameterListElementType();
    JavaTypeParameterElementType TYPE_PARAMETER = new JavaTypeParameterElementType();
    JavaTypeParameterListElementType TYPE_PARAMETER_LIST = new JavaTypeParameterListElementType();
    JavaClassInitializerElementType CLASS_INITIALIZER = new JavaClassInitializerElementType();
    JavaImportListElementType IMPORT_LIST = new JavaImportListElementType();

    JavaParameterElementType PARAMETER = new JavaParameterElementType("PARAMETER") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new ParameterElement(JavaElementType.PARAMETER);
        }
    };
    JavaParameterElementType RECEIVER_PARAMETER = new JavaParameterElementType("RECEIVER") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new ParameterElement(JavaElementType.RECEIVER_PARAMETER);
        }
    };

    JavaClassElementType CLASS = new JavaClassElementType("CLASS") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new ClassElement(this);
        }
    };
    JavaClassElementType ANONYMOUS_CLASS = new JavaClassElementType("ANONYMOUS_CLASS") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new AnonymousClassElement();
        }
    };
    JavaClassElementType ENUM_CONSTANT_INITIALIZER = new JavaClassElementType("ENUM_CONSTANT_INITIALIZER") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new EnumConstantInitializerElement();
        }
    };

    JavaMethodElementType METHOD = new JavaMethodElementType("METHOD") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new MethodElement();
        }
    };
    JavaMethodElementType ANNOTATION_METHOD = new JavaMethodElementType("ANNOTATION_METHOD") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new AnnotationMethodElement();
        }
    };

    JavaFieldStubElementType FIELD = new JavaFieldStubElementType("FIELD") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new FieldElement();
        }
    };
    JavaFieldStubElementType ENUM_CONSTANT = new JavaFieldStubElementType("ENUM_CONSTANT") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new EnumConstantElement();
        }
    };

    JavaClassReferenceListElementType EXTENDS_LIST = new JavaClassReferenceListElementType("EXTENDS_LIST") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new ExtendsListElement();
        }
    };
    JavaClassReferenceListElementType IMPLEMENTS_LIST = new JavaClassReferenceListElementType("IMPLEMENTS_LIST") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new ImplementsListElement();
        }
    };
    JavaClassReferenceListElementType THROWS_LIST = new JavaClassReferenceListElementType("THROWS_LIST") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new PsiThrowsListImpl();
        }
    };
    JavaClassReferenceListElementType EXTENDS_BOUND_LIST = new JavaClassReferenceListElementType("EXTENDS_BOUND_LIST") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new TypeParameterExtendsBoundsListElement();
        }
    };

    JavaImportStatementElementType IMPORT_STATEMENT = new JavaImportStatementElementType("IMPORT_STATEMENT") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new ImportStatementElement();
        }
    };
    JavaImportStatementElementType IMPORT_STATIC_STATEMENT = new JavaImportStatementElementType("IMPORT_STATIC_STATEMENT") {
        
        @Override
        public ASTNode createCompositeNode() {
            return new ImportStaticStatementElement();
        }
    };

    IStubFileElementType JAVA_FILE = new JavaFileElementType();
}