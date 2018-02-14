package com.gome.maven.codeInsight.completion;

import com.gome.maven.openapi.util.text.StringUtil;

/**
 * @author yole
 */
public class PlainPrefixMatcher extends PrefixMatcher {

    public PlainPrefixMatcher(String prefix) {
        super(prefix);
    }

    @Override
    public boolean isStartMatch(String name) {
        return StringUtil.startsWithIgnoreCase(name, getPrefix());
    }

    @Override
    public boolean prefixMatches( String name) {
        return StringUtil.containsIgnoreCase(name, getPrefix());
    }

    
    @Override
    public PrefixMatcher cloneWithPrefix( String prefix) {
        return new PlainPrefixMatcher(prefix);
    }
}
