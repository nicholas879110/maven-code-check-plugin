package com.gome.maven.codeInsight.completion.impl;

import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupElementWeigher;
import com.gome.maven.codeInsight.lookup.WeighingContext;

/**
 * @author Peter
 */
public class PreferStartMatching extends LookupElementWeigher {

    public PreferStartMatching() {
        super("middleMatching", false, true);
    }

    @Override
    public Comparable weigh( LookupElement element,  WeighingContext context) {
        return !CompletionServiceImpl.isStartMatch(element, context);
    }
}
