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

/**
 * Created by IntelliJ IDEA.
 * User: igork
 * Date: Nov 25, 2002
 * Time: 1:44:25 PM
 * To change this template use Options | File Templates.
 */
package com.gome.maven.codeInsight.completion.proc;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiModifier;
import com.gome.maven.psi.PsiVariable;
import com.gome.maven.psi.ResolveState;
import com.gome.maven.psi.scope.BaseScopeProcessor;
import com.gome.maven.psi.scope.ElementClassHint;
import com.gome.maven.psi.scope.JavaScopeProcessorEvent;

import java.util.ArrayList;
import java.util.List;


/** Simple processor to get all visible variables
 * @see com.gome.maven.psi.scope.util.PsiScopesUtil
 */
public class VariablesProcessor
        extends BaseScopeProcessor implements ElementClassHint{
    private final String myPrefix;
    private boolean myStaticScopeFlag = false;
    private final boolean myStaticSensitiveFlag;
    private final List<PsiVariable> myResultList;

    /** Collecting _all_ variables in scope */
    public VariablesProcessor(String _prefix, boolean staticSensitiveFlag){
        this(_prefix, staticSensitiveFlag, new ArrayList<PsiVariable>());
    }

    /** Collecting _all_ variables in scope */
    public VariablesProcessor(String _prefix, boolean staticSensitiveFlag, List<PsiVariable> lst){
        myPrefix = _prefix;
        myStaticSensitiveFlag = staticSensitiveFlag;
        myResultList = lst;
    }

    @Override
    public boolean shouldProcess(DeclarationKind kind) {
        return kind == DeclarationKind.VARIABLE || kind == DeclarationKind.FIELD || kind == DeclarationKind.ENUM_CONST;
    }

    /** Always return true since we wanna get all vars in scope */
    @Override
    public boolean execute( PsiElement pe,  ResolveState state){
        if(pe instanceof PsiVariable){
            final PsiVariable pvar = (PsiVariable)pe;
            final String pvar_name = pvar.getName();
            if(pvar_name.startsWith(myPrefix)){
                if(!myStaticSensitiveFlag || (!myStaticScopeFlag || pvar.hasModifierProperty(PsiModifier.STATIC))){
                    myResultList.add(pvar);
                }
            }
        }

        return true;
    }

    @Override
    public final void handleEvent( Event event, Object associated){
        if(event == JavaScopeProcessorEvent.START_STATIC)
            myStaticScopeFlag = true;
    }

    /** sometimes it is important to get results as array */
    public PsiVariable[] getResultsAsArray(){
        PsiVariable[] ret = new PsiVariable[myResultList.size()];
        myResultList.toArray(ret);
        return ret;
    }

    @Override
    public <T> T getHint( Key<T> hintKey) {
        if (hintKey == ElementClassHint.KEY) {
            return (T)this;
        }

        return super.getHint(hintKey);
    }
}
