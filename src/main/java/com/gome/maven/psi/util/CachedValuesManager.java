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
package com.gome.maven.psi.util;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.NotNullLazyKey;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.UserDataHolderEx;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.concurrent.ConcurrentMap;

/**
 * A service used to create and store {@link com.gome.maven.psi.util.CachedValue} objects.<p/>
 *
 * By default cached values are stored in the user data of associated objects implementing {@link com.gome.maven.openapi.util.UserDataHolder}.
 *
 * @see #createCachedValue(CachedValueProvider, boolean)
 * @see #getCachedValue(com.gome.maven.psi.PsiElement, CachedValueProvider)
 * @see #getCachedValue(com.gome.maven.openapi.util.UserDataHolder, CachedValueProvider)
 */
public abstract class CachedValuesManager {
    private static final NotNullLazyKey<CachedValuesManager, Project> INSTANCE_KEY = ServiceManager.createLazyKey(CachedValuesManager.class);

    public static CachedValuesManager getManager( Project project) {
        return INSTANCE_KEY.getValue(project);
    }

    /**
     * Creates new CachedValue instance with given provider. If the return value is marked as trackable, it's treated as
     * yet another dependency and must comply its specification. See {@link com.gome.maven.psi.util.CachedValueProvider.Result#getDependencyItems()} for
     * the details.
     *
     * @param provider computes values.
     * @param trackValue if value tracking required. T should be trackable in this case.
     * @return new CachedValue instance.
     */
    
    public abstract <T> CachedValue<T> createCachedValue( CachedValueProvider<T> provider, boolean trackValue);
    
    public abstract <T,P> ParameterizedCachedValue<T,P> createParameterizedCachedValue( ParameterizedCachedValueProvider<T,P> provider, boolean trackValue);

    /**
     * Rarely needed because it tracks the return value as a dependency.
     * @return a CachedValue like in {@link #createCachedValue(CachedValueProvider, boolean)}, with trackable return value.
     */
    
    public <T> CachedValue<T> createCachedValue( CachedValueProvider<T> provider) {
        return createCachedValue(provider, true);
    }

    public <T, D extends UserDataHolder, P> T getParameterizedCachedValue( D dataHolder,
                                                                           Key<ParameterizedCachedValue<T,P>> key,
                                                                           ParameterizedCachedValueProvider<T, P> provider,
                                                                          boolean trackValue,
                                                                          P parameter) {
        ParameterizedCachedValue<T,P> value;

        if (dataHolder instanceof UserDataHolderEx) {
            UserDataHolderEx dh = (UserDataHolderEx)dataHolder;
            value = dh.getUserData(key);
            if (value == null) {
                value = createParameterizedCachedValue(provider, trackValue);
                value = dh.putUserDataIfAbsent(key, value);
            }
        }
        else {
            synchronized (dataHolder) {
                value = dataHolder.getUserData(key);
                if (value == null) {
                    value = createParameterizedCachedValue(provider, trackValue);
                    dataHolder.putUserData(key, value);
                }
            }
        }
        return value.getValue(parameter);
    }

    /**
     * Utility method storing created cached values in a {@link com.gome.maven.openapi.util.UserDataHolder}.
     *
     * @param dataHolder holder to store the cached value, e.g. a PsiElement.
     * @param key key to store the cached value.
     * @param provider provider creating the cached value.
     * @param trackValue if value tracking required. T should be trackable in this case.
     * @return up-to-date value.
     */
    public abstract <T, D extends UserDataHolder> T getCachedValue( D dataHolder,
                                                                    Key<CachedValue<T>> key,
                                                                    CachedValueProvider<T> provider,
                                                                   boolean trackValue);

    /**
     * Create a cached value with the given provider and non-tracked return value, store it in the first argument's user data. If it's already stored, reuse it.
     * @return The cached value
     */
    public <T, D extends UserDataHolder> T getCachedValue( D dataHolder,  CachedValueProvider<T> provider) {
        return getCachedValue(dataHolder, this.<T>getKeyForClass(provider.getClass()), provider, false);
    }

    /**
     * Create a cached value with the given provider and non-tracked return value, store it in PSI element's user data. If it's already stored, reuse it.
     * @return The cached value
     */
    public static <T> T getCachedValue( final PsiElement psi,  final CachedValueProvider<T> provider) {
        CachedValuesManager manager = getManager(psi.getProject());
        return manager.getCachedValue(psi, manager.<T>getKeyForClass(provider.getClass()), new CachedValueProvider<T>() {
            
            @Override
            public Result<T> compute() {
                Result<T> result = provider.compute();
                if (result != null && !psi.isPhysical()) {
                    return Result.create(result.getValue(), ArrayUtil.append(result.getDependencyItems(), psi));
                }
                return result;
            }
        }, false);
    }

    private final ConcurrentMap<String, Key<CachedValue>> keyForProvider = ContainerUtil.newConcurrentMap();
    
    public <T> Key<CachedValue<T>> getKeyForClass( Class<?> providerClass) {
        String name = providerClass.getName();
        assert name != null : providerClass + " doesn't have a name; can't be used for cache value provider";
        Key<CachedValue> key = keyForProvider.get(name);
        if (key == null) {
            key = ConcurrencyUtil.cacheOrGet(keyForProvider, name, Key.<CachedValue>create(name));
        }
        //noinspection unchecked
        return (Key)key;
    }
}
