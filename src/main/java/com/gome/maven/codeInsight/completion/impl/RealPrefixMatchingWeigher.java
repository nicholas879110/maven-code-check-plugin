package com.gome.maven.codeInsight.completion.impl;

import com.gome.maven.codeInsight.completion.PrefixMatcher;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupElementWeigher;
import com.gome.maven.codeInsight.lookup.WeighingContext;

/**
 * @author Peter
 */
public class RealPrefixMatchingWeigher extends LookupElementWeigher {

    public RealPrefixMatchingWeigher() {
        super("prefix", false, true);
    }

    @Override
    public Comparable weigh( LookupElement element,  WeighingContext context) {
        return getBestMatchingDegree(element, CompletionServiceImpl.getItemMatcher(element, context));
    }

    public static int getBestMatchingDegree(LookupElement element, PrefixMatcher matcher) {
        int max = Integer.MIN_VALUE;
        for (String lookupString : element.getAllLookupStrings()) {
            max = Math.max(max, matcher.matchingDegree(lookupString));
        }
        return -max;
    }
}
