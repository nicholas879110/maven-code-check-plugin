/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.gome.maven.refactoring.util;

import com.gome.maven.psi.ElementDescriptionLocation;
import com.gome.maven.psi.ElementDescriptionProvider;

/**
 * @author yole
 */
public class NonCodeSearchDescriptionLocation extends ElementDescriptionLocation {
    private final boolean myNonJava;

    private NonCodeSearchDescriptionLocation(final boolean nonJava) {
        myNonJava = nonJava;
    }

    public static final NonCodeSearchDescriptionLocation NON_JAVA = new NonCodeSearchDescriptionLocation(true);
    public static final NonCodeSearchDescriptionLocation STRINGS_AND_COMMENTS = new NonCodeSearchDescriptionLocation(false);

    @Override
    public ElementDescriptionProvider getDefaultProvider() {
        return DefaultNonCodeSearchElementDescriptionProvider.INSTANCE;
    }

    public boolean isNonJava() {
        return myNonJava;
    }
}
