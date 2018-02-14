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

package com.gome.maven.psi.impl.source.resolve.reference.impl.providers;

import com.gome.maven.util.ProcessingContext;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiReference;

import java.util.Map;


/**
 * @author Maxim.Mossienko
 */
public interface CustomizableReferenceProvider {
    public final class CustomizationKey<T> {
        private final String myOptionDescription;

        public CustomizationKey( String optionDescription) {
            myOptionDescription = optionDescription;
        }

        public String toString() { return myOptionDescription; }

        public T getValue( Map<CustomizationKey,Object> options) {
            return options == null ? null : (T)options.get(this);
        }

        public boolean getBooleanValue( Map<CustomizationKey,Object> options) {
            if (options == null) {
                return false;
            }
            final Boolean o = (Boolean)options.get(this);
            return o != null && o.booleanValue();
        }

        public void putValue(Map<CustomizationKey,Object> options, T value) {
            options.put(this, value);
        }
    }

    void setOptions( Map<CustomizationKey,Object> options);
     Map<CustomizationKey,Object> getOptions();

    
    public abstract PsiReference[] getReferencesByElement( PsiElement element,  final ProcessingContext matchingContext);
}
