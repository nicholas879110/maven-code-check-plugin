package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.LookupElement;

/**
 * @author peter
 */
public abstract class PrefixMatcher {
    public static final PrefixMatcher ALWAYS_TRUE = new PlainPrefixMatcher("");
    protected final String myPrefix;

    protected PrefixMatcher(String prefix) {
        myPrefix = prefix;
    }

    public boolean prefixMatches( LookupElement element) {
        for (String s : element.getAllLookupStrings()) {
            if (prefixMatches(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStartMatch(LookupElement element) {
        for (String s : element.getAllLookupStrings()) {
            if (isStartMatch(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStartMatch(String name) {
        return prefixMatches(name);
    }

    public abstract boolean prefixMatches( String name);

    
    public final String getPrefix() {
        return myPrefix;
    }

     public abstract PrefixMatcher cloneWithPrefix( String prefix);

    public int matchingDegree(String string) {
        return 0;
    }
}
