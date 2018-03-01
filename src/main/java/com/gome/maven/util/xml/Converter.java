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
package com.gome.maven.util.xml;

import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.ide.IdeBundle;

/**
 * Base DOM class to convert objects of a definite type into {@link String} and back. Most often used with
 * {@link com.gome.maven.util.xml.Convert} annotation with methods returning {@link com.gome.maven.util.xml.GenericDomValue}&lt;T&gt;.
 *
 * @see com.gome.maven.util.xml.ResolvingConverter
 * @see com.gome.maven.util.xml.CustomReferenceConverter
 *
 * @param <T> Type to convert from/to.
 *
 * @author peter
 */
public abstract class Converter<T> {
    
    public abstract T fromString(  String s, final ConvertContext context);
    
    public abstract String toString( T t, final ConvertContext context);

    /**
     * @param s string value that couldn't be resolved
     * @param context context
     * @return error message used to highlight the errors somewhere in the UI, most often - like unresolved references in XML
     */
    
    public String getErrorMessage( String s, final ConvertContext context) {
        return CodeInsightBundle.message("error.cannot.convert.default.message", s);
    }


    /**
     * @deprecated {@link com.gome.maven.util.xml.converters.values.NumberValueConverter}
     */
    @Deprecated
    public static final Converter<Integer> INTEGER_CONVERTER = new Converter<Integer>() {
        @Override
        public Integer fromString(final String s, final ConvertContext context) {
            if (s == null) return null;
            try {
                return Integer.decode(s);
            }
            catch (Exception e) {
                return null;
            }
        }

        @Override
        public String toString(final Integer t, final ConvertContext context) {
            return t == null? null: t.toString();
        }

        @Override
        public String getErrorMessage(final String s, final ConvertContext context) {
            return IdeBundle.message("value.should.be.integer");
        }
    };

    @Deprecated
    public static final Converter<String> EMPTY_CONVERTER = new Converter<String>() {
        @Override
        public String fromString(final String s, final ConvertContext context) {
            return s;
        }

        @Override
        public String toString(final String t, final ConvertContext context) {
            return t;
        }

    };

}
