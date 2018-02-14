/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.LookupElement;

/**
 * @author peter
 */
public class CompletionResult {
    private final LookupElement myLookupElement;
    private final PrefixMatcher myMatcher;
    private final CompletionSorter mySorter;

    private CompletionResult(LookupElement lookupElement, PrefixMatcher matcher, CompletionSorter sorter) {
        myLookupElement = lookupElement;
        myMatcher = matcher;
        mySorter = sorter;
    }

    
    public static CompletionResult wrap(LookupElement lookupElement, PrefixMatcher matcher, CompletionSorter sorter) {
        if (matcher.prefixMatches(lookupElement)) {
            return new CompletionResult(lookupElement, matcher, sorter);
        }
        return null;
    }

    public PrefixMatcher getPrefixMatcher() {
        return myMatcher;
    }

    public CompletionSorter getSorter() {
        return mySorter;
    }

    public LookupElement getLookupElement() {
        return myLookupElement;
    }

    
    public CompletionResult withLookupElement( LookupElement element) {
        if (!myMatcher.prefixMatches(element)) {
            throw new AssertionError("The new element doesn't match the prefix");
        }
        return new CompletionResult(element, myMatcher, mySorter);
    }

    public boolean isStartMatch() {
        return myMatcher.isStartMatch(myLookupElement);
    }

}
