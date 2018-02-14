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
package com.gome.maven.openapi.fileTypes;



/**
 * @author peter
 */
public abstract class FileNameMatcherEx implements FileNameMatcher {
    @Override
    public boolean accept(  String fileName) {
        return acceptsCharSequence(fileName);
    }

    public abstract boolean acceptsCharSequence( CharSequence fileName);

    public static boolean acceptsCharSequence( FileNameMatcher matcher,  CharSequence fileName) {
        if (matcher instanceof FileNameMatcherEx) {
            return ((FileNameMatcherEx)matcher).acceptsCharSequence(fileName);
        }
        return matcher.accept(fileName.toString());
    }
}