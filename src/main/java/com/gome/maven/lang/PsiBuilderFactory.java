/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.lang;

import com.gome.maven.lexer.Lexer;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;

/**
 * @author yole
 */
public abstract class PsiBuilderFactory {
    public static PsiBuilderFactory getInstance() {
        return ServiceManager.getService(PsiBuilderFactory.class);
    }

    
    public abstract PsiBuilder createBuilder( Project project,  ASTNode chameleon);

    
    public abstract PsiBuilder createBuilder( Project project,  LighterLazyParseableNode chameleon);

    
    public abstract PsiBuilder createBuilder( Project project,
                                              ASTNode chameleon,
                                              Lexer lexer,
                                              Language lang,
                                              CharSequence seq);

    
    public abstract PsiBuilder createBuilder( Project project,
                                              LighterLazyParseableNode chameleon,
                                              Lexer lexer,
                                              Language lang,
                                              CharSequence seq);

    
    public abstract PsiBuilder createBuilder( ParserDefinition parserDefinition,  Lexer lexer,  CharSequence seq);
}
