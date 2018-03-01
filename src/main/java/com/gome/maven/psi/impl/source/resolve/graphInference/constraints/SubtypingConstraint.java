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
package com.gome.maven.psi.impl.source.resolve.graphInference.constraints;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.PsiCapturedWildcardType;
import com.gome.maven.psi.PsiSubstitutor;
import com.gome.maven.psi.PsiType;
import com.gome.maven.psi.PsiWildcardType;
import com.gome.maven.psi.impl.source.resolve.graphInference.InferenceSession;

import java.util.List;

public class SubtypingConstraint implements ConstraintFormula {
    private static final Logger LOG = Logger.getInstance("#" + SubtypingConstraint.class.getName());

    private PsiType myS;
    private PsiType myT;

    public SubtypingConstraint(PsiType t, PsiType s) {
        myT = t;
        myS = s;
    }

    @Override
    public void apply(PsiSubstitutor substitutor, boolean cache) {
        myT = substitutor.substitute(myT);
        myS = substitutor.substitute(myS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubtypingConstraint that = (SubtypingConstraint)o;

        if ( myS instanceof PsiCapturedWildcardType && myS != that.myS) return false;

        if (myS != null ? !myS.equals(that.myS) : that.myS != null) return false;
        if (myT != null ? !myT.equals(that.myT) : that.myT != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = myS != null ? myS.hashCode() : 0;
        result = 31 * result + (myT != null ? myT.hashCode() : 0);
        return result;
    }

    @Override
    public boolean reduce(InferenceSession session, List<ConstraintFormula> constraints) {
        if (myT instanceof PsiWildcardType) {
            PsiType tBound = ((PsiWildcardType)myT).getBound();
            if (tBound == null) {
                return true;
            }

            if (tBound instanceof PsiCapturedWildcardType) {
                tBound = ((PsiWildcardType)myT).isExtends() ? ((PsiCapturedWildcardType)tBound).getUpperBound()
                        : ((PsiCapturedWildcardType)tBound).getLowerBound();
            }
            if (myS instanceof PsiCapturedWildcardType) {
                myS = ((PsiCapturedWildcardType)myS).getWildcard();
            }

            if (((PsiWildcardType)myT).isExtends()) {
                if (myS instanceof PsiWildcardType) {
                    final PsiType sBound = ((PsiWildcardType)myS).getBound();
                    if (sBound == null) {
                        constraints.add(new StrictSubtypingConstraint(tBound, ((PsiWildcardType)myS).getExtendsBound()));
                        return true;
                    }

                    if (((PsiWildcardType)myS).isExtends()) {
                        if (sBound instanceof PsiCapturedWildcardType) {
                            return true;
                        }
                        constraints.add(new StrictSubtypingConstraint(tBound, sBound));
                        return true;
                    }

                    if (((PsiWildcardType)myS).isSuper()) {
                        constraints.add(new TypeEqualityConstraint(tBound, PsiType.getJavaLangObject(((PsiWildcardType)myT).getManager(), myT.getResolveScope())));
                        return true;
                    }

                    assert false;
                }
                else {
                    constraints.add(new StrictSubtypingConstraint(tBound, myS));
                    return true;
                }
            }
            else {
                LOG.assertTrue(((PsiWildcardType)myT).isSuper());

                if (myS instanceof PsiWildcardType) {
                    final PsiType sBound = ((PsiWildcardType)myS).getBound();
                    if (sBound != null && ((PsiWildcardType)myS).isSuper()) {
                        if (sBound instanceof PsiCapturedWildcardType) {
                            return false;
                        }
                        constraints.add(new StrictSubtypingConstraint(sBound, tBound));
                        return true;
                    }
                } else {
                    constraints.add(new StrictSubtypingConstraint(myS, tBound));
                    return true;
                }
            }
            return false;
        } else {
            if (myS instanceof PsiWildcardType) {
                return false;
            } else {
                constraints.add(new TypeEqualityConstraint(myT, myS));
                return true;
            }
        }
    }

    @Override
    public String toString() {
        return myS.getPresentableText() + " <= " + myT.getPresentableText();
    }
}
