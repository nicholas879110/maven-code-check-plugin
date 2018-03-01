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
package com.gome.maven.psi;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.search.GlobalSearchScope;

/**
 * Represents a wildcard type, with bounds.
 *
 * @author dsl
 */
public class PsiWildcardType extends PsiType.Stub {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.PsiWildcardType");

    private static final Key<PsiWildcardType> UNBOUNDED_WILDCARD = new Key<PsiWildcardType>("UNBOUNDED_WILDCARD");
     private static final String EXTENDS_PREFIX = "? extends ";
     private static final String SUPER_PREFIX = "? super ";

    
    private final PsiManager myManager;
    private final boolean myIsExtending;
    private final PsiType myBound;

    private PsiWildcardType( PsiManager manager, boolean isExtending,  PsiType bound) {
        super(PsiAnnotation.EMPTY_ARRAY);
        myManager = manager;
        myIsExtending = isExtending;
        myBound = bound;
    }

    private PsiWildcardType( PsiWildcardType type,  PsiAnnotation[] annotations) {
        super(annotations);
        myManager = type.myManager;
        myIsExtending = type.myIsExtending;
        myBound = type.myBound;
    }

    
    public static PsiWildcardType createUnbounded( PsiManager manager) {
        PsiWildcardType unboundedWildcard = manager.getUserData(UNBOUNDED_WILDCARD);
        if (unboundedWildcard == null) {
            unboundedWildcard = manager.putUserDataIfAbsent(UNBOUNDED_WILDCARD, new PsiWildcardType(manager, false, null));
        }
        return unboundedWildcard;
    }

    
    public static PsiWildcardType createExtends( PsiManager manager,  PsiType bound) {
        LOG.assertTrue(!(bound instanceof PsiWildcardType));
        LOG.assertTrue(bound != PsiType.NULL);
        return new PsiWildcardType(manager, true, bound);
    }

    
    public static PsiWildcardType createSuper( PsiManager manager,  PsiType bound) {
        LOG.assertTrue(!(bound instanceof PsiWildcardType));
        LOG.assertTrue(bound != PsiType.NULL);
        return new PsiWildcardType(manager, false, bound);
    }

    
    public PsiWildcardType annotate( PsiAnnotation[] annotations) {
        return annotations.length == 0 ? this : new PsiWildcardType(this, annotations);
    }

    
    @Override
    public String getPresentableText() {
        return getText(false, true, myBound == null ? null : myBound.getPresentableText());
    }

    @Override
    
    public String getCanonicalText(boolean annotated) {
        return getText(true, annotated, myBound == null ? null : myBound.getCanonicalText(annotated));
    }

    
    @Override
    public String getInternalCanonicalText() {
        return getText(true, true, myBound == null ? null : myBound.getInternalCanonicalText());
    }

    private String getText(boolean qualified, boolean annotated,  String suffix) {
        PsiAnnotation[] annotations = getAnnotations();
        if ((!annotated || annotations.length == 0) && suffix == null) return "?";

        StringBuilder sb = new StringBuilder();
        if (annotated) {
            PsiNameHelper.appendAnnotations(sb, annotations, qualified);
        }
        if (suffix == null) {
            sb.append('?');
        }
        else {
            sb.append(myIsExtending ? EXTENDS_PREFIX : SUPER_PREFIX);
            sb.append(suffix);
        }
        return sb.toString();
    }

    @Override
    
    public GlobalSearchScope getResolveScope() {
        if (myBound != null) {
            GlobalSearchScope scope = myBound.getResolveScope();
            if (scope != null) {
                return scope;
            }
        }
        return GlobalSearchScope.allScope(myManager.getProject());
    }

    @Override
    
    public PsiType[] getSuperTypes() {
        return new PsiType[]{getExtendsBound()};
    }

    @Override
    public boolean equalsToText( String text) {
        if (myBound == null) return "?".equals(text);
        if (myIsExtending) {
            return text.startsWith(EXTENDS_PREFIX) && myBound.equalsToText(text.substring(EXTENDS_PREFIX.length()));
        }
        else {
            return text.startsWith(SUPER_PREFIX) && myBound.equalsToText(text.substring(SUPER_PREFIX.length()));
        }
    }
    
    public PsiManager getManager() {
        return myManager;
    }

    public boolean equals(Object o) {
        if (!(o instanceof PsiWildcardType)) return false;

        PsiWildcardType that = (PsiWildcardType)o;
        if (myBound == null && that.myBound != null) {
            return that.isExtends() && that.myBound.equalsToText(CommonClassNames.JAVA_LANG_OBJECT);
        }
        else if (myBound != null && that.myBound == null) {
            return isExtends() && myBound.equalsToText(CommonClassNames.JAVA_LANG_OBJECT);
        }
        return myIsExtending == that.myIsExtending && Comparing.equal(myBound, that.myBound);
    }

    public int hashCode() {
        return (myIsExtending ? 1 : 0) + (myBound != null ? myBound.hashCode() : 0);
    }

    /**
     * Use this method to obtain a bound of wildcard type.
     *
     * @return <code>null</code> if unbounded, a bound otherwise.
     */
    
    public PsiType getBound() {
        return myBound;
    }

    @Override
    public <A> A accept( PsiTypeVisitor<A> visitor) {
        return visitor.visitWildcardType(this);
    }

    @Override
    public boolean isValid() {
        return myBound == null || myBound.isValid();
    }

    /**
     * Returns whether this is a lower bound (<code>? extends XXX</code>).
     *
     * @return <code>true</code> for <code>extends</code> wildcards, <code>false</code> for <code>super</code>
     *         and unbounded wildcards.
     */
    public boolean isExtends() {
        return myBound != null && myIsExtending;
    }

    /**
     * Returns whether this is an upper bound (<code>? super XXX</code>).
     *
     * @return <code>true</code> for <code>super</code> wildcards, <code>false</code> for <code>extends</code>
     *         and unbounded wildcards.
     */
    public boolean isSuper() {
        return myBound != null && !myIsExtending;
    }

    /**
     * @return false for unbounded wildcards, true otherwise
     */
    public boolean isBounded() {
        return myBound != null;
    }

    /**
     * A lower bound that this wildcard imposes on type parameter value.<br>
     * That is:<br>
     * <ul>
     * <li> for <code>? extends XXX</code>: <code>XXX</code>
     * <li> for <code>? super XXX</code>: <code>java.lang.Object</code>
     * <li> for <code>?</code>: <code>java.lang.Object</code>
     * </ul>
     *
     * @return <code>PsiType</code> representing a lower bound. Never returns <code>null</code>.
     */
    
    public PsiType getExtendsBound() {
        if (myBound == null || !myIsExtending) {
            return getJavaLangObject(myManager, getResolveScope());
        }
        return myBound;
    }

    /**
     * An upper bound that this wildcard imposes on type parameter value.<br>
     * That is:<br>
     * <ul>
     * <li> for <code>? extends XXX</code>: null type
     * <li> for <code>? super XXX</code>: <code>XXX</code>
     * <li> for <code>?</code>: null type
     * </ul>
     *
     * @return <code>PsiType</code> representing an upper bound. Never returns <code>null</code>.
     */
    
    public PsiType getSuperBound() {
        return myBound == null || myIsExtending ? NULL : myBound;
    }
}
