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
package com.gome.maven.refactoring.listeners;

import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.HashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RefactoringEventData extends UserDataHolderBase {
    public static final Key<Collection<? extends String>> CONFLICTS_KEY = Key.create("conflicts");
    public static final Key<PsiElement> PSI_ELEMENT_KEY = Key.create("element");
    public static final Key<String[]> STRING_PROPERTIES = Key.create("stringProperties");
    public static final Key<PsiElement[]> PSI_ELEMENT_ARRAY_KEY = Key.create("elementArray");
    public static final Key<Collection<UsageInfo>> USAGE_INFOS_KEY = Key.create("usageInfos");

    public void addElement(PsiElement element) {
        putUserData(PSI_ELEMENT_KEY, element);
    }

    public <T> void addMembers(T[] members, Function<T, PsiElement> converter) {
        List<PsiElement> elements = new ArrayList<PsiElement>();
        for (T info : members) {
            elements.add(converter.fun(info));
        }
        addElements(elements);
    }

    public void addElements(Collection<PsiElement> elements) {
        putUserData(PSI_ELEMENT_ARRAY_KEY, elements.toArray(new PsiElement[elements.size()]));
    }

    public void addElements(PsiElement[] elements) {
        putUserData(PSI_ELEMENT_ARRAY_KEY, elements);
    }

    public void addUsages(Collection<UsageInfo> usageInfos) {
        putUserData(USAGE_INFOS_KEY, usageInfos);
    }

    public void addStringProperties(String... properties) {
        putUserData(STRING_PROPERTIES, properties);
    }
}
