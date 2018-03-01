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

/*
 * @author max
 */
package com.gome.maven.psi.impl.source.resolve;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.openapi.util.RecursionGuard;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.AnyPsiChangeListener;
import com.gome.maven.psi.impl.PsiManagerImpl;
import com.gome.maven.psi.impl.source.PsiClassReferenceType;
import com.gome.maven.psi.impl.source.PsiImmediateClassType;
import com.gome.maven.psi.infos.MethodCandidateInfo;
import com.gome.maven.psi.util.TypeConversionUtil;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBus;

import java.lang.ref.Reference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class JavaResolveCache {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.resolve.JavaResolveCache");

    private static final NotNullLazyKey<JavaResolveCache, Project> INSTANCE_KEY = ServiceManager.createLazyKey(JavaResolveCache.class);

    public static JavaResolveCache getInstance(Project project) {
        return INSTANCE_KEY.getValue(project);
    }

    private final ConcurrentMap<PsiExpression, Reference<PsiType>> myCalculatedTypes = ContainerUtil.createConcurrentWeakMap();

    private final Map<PsiVariable,Object> myVarToConstValueMapPhysical = ContainerUtil.createConcurrentWeakMap();
    private final Map<PsiVariable,Object> myVarToConstValueMapNonPhysical = ContainerUtil.createConcurrentWeakMap();

    private static final Object NULL = Key.create("NULL");

    public JavaResolveCache(/*("can be null in com.gome.maven.core.JavaCoreApplicationEnvironment.JavaCoreApplicationEnvironment")*/ MessageBus messageBus) {
        if (messageBus != null) {
            messageBus.connect().subscribe(PsiManagerImpl.ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener.Adapter() {
                @Override
                public void beforePsiChanged(boolean isPhysical) {
                    clearCaches(isPhysical);
                }
            });
        }
    }

    private void clearCaches(boolean isPhysical) {
        myCalculatedTypes.clear();
        if (isPhysical) {
            myVarToConstValueMapPhysical.clear();
        }
        myVarToConstValueMapNonPhysical.clear();
    }

    
    public <T extends PsiExpression> PsiType getType( T expr,  Function<T, PsiType> f) {
        final boolean isOverloadCheck = MethodCandidateInfo.isOverloadCheck();
        PsiType type = !isOverloadCheck ? getCachedType(expr) : null;
        if (type == null) {
            final RecursionGuard.StackStamp dStackStamp = PsiDiamondType.ourDiamondGuard.markStack();
            final RecursionGuard.StackStamp gStackStamp = PsiResolveHelper.ourGraphGuard.markStack();
            type = f.fun(expr);
            if (!dStackStamp.mayCacheNow() || !gStackStamp.mayCacheNow() || isOverloadCheck) {
                return type;
            }
            if (type == null) type = TypeConversionUtil.NULL_TYPE;
            Reference<PsiType> ref = new SoftReference<PsiType>(type);
            myCalculatedTypes.put(expr, ref);

            if (type instanceof PsiClassReferenceType) {
                // convert reference-based class type to the PsiImmediateClassType, since the reference may become invalid
                PsiClassType.ClassResolveResult result = ((PsiClassReferenceType)type).resolveGenerics();
                PsiClass psiClass = result.getElement();
                type = psiClass == null
                        ? type // for type with unresolved reference, leave it in the cache
                        // for clients still might be able to retrieve its getCanonicalText() from the reference text
                        : new PsiImmediateClassType(psiClass, result.getSubstitutor(), ((PsiClassReferenceType)type).getLanguageLevel(), type.getAnnotations());
            }
        }

        if (!type.isValid()) {
            if (expr.isValid()) {
                PsiJavaCodeReferenceElement refInside = type instanceof PsiClassReferenceType ? ((PsiClassReferenceType)type).getReference() : null;
                 String typeinfo = type + " (" + type.getClass() + ")" + (refInside == null ? "" : "; ref inside: "+refInside + " ("+refInside.getClass()+") valid:"+refInside.isValid());
                LOG.error("Type is invalid: " + typeinfo + "; expr: '" + expr + "' (" + expr.getClass() + ") is valid");
            }
            else {
                LOG.error("Expression: '"+expr+"' is invalid, must not be used for getType()");
            }
        }

        return type == TypeConversionUtil.NULL_TYPE ? null : type;
    }

    private <T extends PsiExpression> PsiType getCachedType(T expr) {
        Reference<PsiType> reference = myCalculatedTypes.get(expr);
        return SoftReference.dereference(reference);
    }

    
    public Object computeConstantValueWithCaching( PsiVariable variable,  ConstValueComputer computer, Set<PsiVariable> visitedVars){
        boolean physical = variable.isPhysical();

        Map<PsiVariable, Object> map = physical ? myVarToConstValueMapPhysical : myVarToConstValueMapNonPhysical;
        Object cached = map.get(variable);
        if (cached == NULL) return null;
        if (cached != null) return cached;

        Object result = computer.execute(variable, visitedVars);
        map.put(variable, result == null ? NULL : result);
        return result;
    }

    public interface ConstValueComputer{
        Object execute( PsiVariable variable, Set<PsiVariable> visitedVars);
    }
}
