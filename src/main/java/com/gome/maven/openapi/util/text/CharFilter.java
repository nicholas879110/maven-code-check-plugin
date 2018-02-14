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

package com.gome.maven.openapi.util.text;

/**
 * @author Dmitry Avdeev
 *
 * @see StringUtil#strip(String, CharFilter)
 * @see StringUtil#findFirst(String, CharFilter)
 */
public interface CharFilter {
    CharFilter WHITESPACE_FILTER = new CharFilter() {
        @Override
        public boolean accept(char ch) {
            return Character.isWhitespace(ch);
        }
    };

    CharFilter NOT_WHITESPACE_FILTER = new CharFilter() {
        @Override
        public boolean accept(char ch) {
            return !Character.isWhitespace(ch);
        }
    };

    public boolean accept(char ch);
}