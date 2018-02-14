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

/*
 * @author max
 */
package com.gome.maven.openapi.fileTypes;

import com.gome.maven.openapi.util.Comparing;

public class ExactFileNameMatcher extends FileNameMatcherEx {
    private final String myFileName;
    private final boolean myIgnoreCase;

    public ExactFileNameMatcher(  final String fileName) {
        myFileName = fileName;
        myIgnoreCase = false;
    }

    public ExactFileNameMatcher(  final String fileName, final boolean ignoreCase) {
        myFileName = fileName;
        myIgnoreCase = ignoreCase;
    }

    @Override
    public boolean acceptsCharSequence(  CharSequence fileName) {
        return Comparing.equal(fileName, myFileName, !myIgnoreCase);
    }

    
    
    public String getPresentableString() {
        return myFileName;
    }

    public String getFileName() {
        return myFileName;
    }

    public boolean isIgnoreCase() {
        return myIgnoreCase;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ExactFileNameMatcher that = (ExactFileNameMatcher)o;

        if (!myFileName.equals(that.myFileName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myFileName.hashCode();
    }

    @Override
    public String toString() {
        return getPresentableString();
    }
}