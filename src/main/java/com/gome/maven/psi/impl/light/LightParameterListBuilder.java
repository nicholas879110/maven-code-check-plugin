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
package com.gome.maven.psi.impl.light;

import com.gome.maven.lang.Language;
import com.gome.maven.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public class LightParameterListBuilder extends LightElement implements PsiParameterList {
    private final List<PsiParameter> myParameters = new ArrayList<PsiParameter>();
    private PsiParameter[] myCachedParameters;

    public LightParameterListBuilder(PsiManager manager, Language language) {
        super(manager, language);
    }

    public void addParameter(PsiParameter parameter) {
        myParameters.add(parameter);
        myCachedParameters = null;
    }

    @Override
    public String toString() {
        return "Light parameter lsit";
    }

    
    @Override
    public PsiParameter[] getParameters() {
        if (myCachedParameters == null) {
            if (myParameters.isEmpty()) {
                myCachedParameters = PsiParameter.EMPTY_ARRAY;
            }
            else {
                myCachedParameters = myParameters.toArray(new PsiParameter[myParameters.size()]);
            }
        }

        return myCachedParameters;
    }

    @Override
    public int getParameterIndex(PsiParameter parameter) {
        return myParameters.indexOf(parameter);
    }

    @Override
    public int getParametersCount() {
        return myParameters.size();
    }

    @Override
    public void accept( PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor) visitor).visitParameterList(this);
        }
    }

}
