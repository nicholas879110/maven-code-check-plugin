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
package com.gome.maven.util.text;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.HashSet;


import java.util.Collection;
import java.util.Set;

/**
 * @author peter
 */
public class UniqueNameGenerator implements Condition<String> {
    private final Set<String> myExistingNames = new HashSet<String>();

    public <T> UniqueNameGenerator( Collection<T> elements,  Function<T, String> namer) {
        for (final T t : elements) {
            addExistingName(namer != null ? namer.fun(t) : t.toString());
        }
    }

    public UniqueNameGenerator() {
    }

    @Override
    public final boolean value(final String candidate) {
        return !myExistingNames.contains(candidate);
    }

    public final boolean isUnique(final String name, String prefix, String suffix) {
        return value(prefix + name + suffix);
    }

    
    public static String generateUniqueName(final String defaultName, final Collection<String> existingNames) {
        return generateUniqueName(defaultName, "", "", existingNames);
    }

    
    public static String generateUniqueName(final String defaultName, final String prefix, final String suffix, final Collection<String> existingNames) {
        return generateUniqueName(defaultName, prefix, suffix, new Condition<String>() {
            @Override
            public boolean value(final String s) {
                return !existingNames.contains(s);
            }
        });
    }

    
    public static String generateUniqueName(final String defaultName, final Condition<String> validator) {
        return generateUniqueName(defaultName, "", "", validator);
    }

    
    public static String generateUniqueName(final String defaultName, final String prefix, final String suffix, final Condition<String> validator) {
        return generateUniqueName(defaultName, prefix, suffix, "", "", validator);
    }

    
    public static String generateUniqueName(final String defaultName, final String prefix, final String suffix,
                                            final String beforeNumber, final String afterNumber, final Condition<String> validator) {
        final String defaultFullName = prefix + defaultName + suffix;
        if (validator.value(defaultFullName)) {
            return defaultFullName;
        }

        for (int i = 2; ; i++) {
            final String fullName = prefix + defaultName + beforeNumber + i + afterNumber + suffix;
            if (validator.value(fullName)) {
                return fullName;
            }
        }
    }

    
    public String generateUniqueName(final String defaultName, final String prefix, final String suffix) {
        return generateUniqueName(defaultName, prefix, suffix, "", "");
    }

    
    public String generateUniqueName(final String defaultName, final String prefix, final String suffix, final String beforeNumber, final String afterNumber) {
        final String result = generateUniqueName(defaultName, prefix, suffix, beforeNumber, afterNumber, this);
        addExistingName(result);
        return result;
    }

    public void addExistingName(String result) {
        myExistingNames.add(result);
    }

    public String generateUniqueName(final String defaultName) {
        return generateUniqueName(defaultName, "", "");
    }
}