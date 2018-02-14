
package com.gome.maven.codeInsight.completion.impl;

import com.gome.maven.codeInsight.CodeInsightSettings;
import com.gome.maven.codeInsight.completion.PrefixMatcher;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.codeStyle.MinusculeMatcher;
import com.gome.maven.psi.codeStyle.NameUtil;
import com.gome.maven.util.containers.FList;
import com.gome.maven.util.text.CharArrayUtil;

/**
 * @author peter
 */
public class CamelHumpMatcher extends PrefixMatcher {
    private final MinusculeMatcher myMatcher;
    private final MinusculeMatcher myCaseInsensitiveMatcher;
    private final boolean myCaseSensitive;
    private static boolean ourForceStartMatching;


    public CamelHumpMatcher( final String prefix) {
        this(prefix, true);
    }

    public CamelHumpMatcher(String prefix, boolean caseSensitive) {
        super(prefix);
        myCaseSensitive = caseSensitive;
        myMatcher = createMatcher(myCaseSensitive);
        myCaseInsensitiveMatcher = createMatcher(false);
    }

    @Override
    public boolean isStartMatch(String name) {
        return myMatcher.isStartMatch(name);
    }

    @Override
    public boolean isStartMatch(LookupElement element) {
        for (String s : element.getAllLookupStrings()) {
            FList<TextRange> ranges = myCaseInsensitiveMatcher.matchingFragments(s);
            if (ranges == null) continue;
            if (ranges.isEmpty() || skipUnderscores(s) >= ranges.get(0).getStartOffset()) {
                return true;
            }
        }

        return false;
    }

    private static int skipUnderscores( String name) {
        return CharArrayUtil.shiftForward(name, 0, "_");
    }

    @Override
    public boolean prefixMatches( final String name) {
        return myMatcher.matches(name);
    }

    @Override
    public boolean prefixMatches( final LookupElement element) {
        return prefixMatchersInternal(element, !element.isCaseSensitive());
    }

    private boolean prefixMatchersInternal(final LookupElement element, final boolean itemCaseInsensitive) {
        for (final String name : element.getAllLookupStrings()) {
            if (itemCaseInsensitive && StringUtil.startsWithIgnoreCase(name, myPrefix) || prefixMatches(name)) {
                return true;
            }
            if (itemCaseInsensitive && CodeInsightSettings.ALL != CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE) {
                if (myCaseInsensitiveMatcher.matches(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    
    public PrefixMatcher cloneWithPrefix( final String prefix) {
        return new CamelHumpMatcher(prefix, myCaseSensitive);
    }

    private MinusculeMatcher createMatcher(final boolean caseSensitive) {
        String prefix = applyMiddleMatching(myPrefix);

        if (!caseSensitive) {
            return NameUtil.buildMatcher(prefix, NameUtil.MatchingCaseSensitivity.NONE);
        }

        switch (CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE) {
            case CodeInsightSettings.NONE:
                return NameUtil.buildMatcher(prefix, NameUtil.MatchingCaseSensitivity.NONE);
            case CodeInsightSettings.FIRST_LETTER:
                return NameUtil.buildMatcher(prefix, NameUtil.MatchingCaseSensitivity.FIRST_LETTER);
            default:
                return NameUtil.buildMatcher(prefix, NameUtil.MatchingCaseSensitivity.ALL);
        }
    }

    public static String applyMiddleMatching(String prefix) {
        if (Registry.is("ide.completion.middle.matching") && !prefix.isEmpty() && !ourForceStartMatching) {
            return "*" + StringUtil.replace(prefix, ".", ". ").trim();
        }
        return prefix;
    }

    @Override
    public String toString() {
        return myPrefix;
    }

    /**
     * In an ideal world, all tests would use the same settings as production, i.e. middle matching.
     * If you see a usage of this method which can be easily removed (i.e. it's easy to make a test pass without it
     * by modifying test expectations slightly), please do it
     */
    
    @Deprecated
    public static void forceStartMatching(Disposable parent) {
        ourForceStartMatching = true;
        Disposer.register(parent, new Disposable() {
            @Override
            public void dispose() {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                ourForceStartMatching = false;
            }
        });
    }

    @Override
    public int matchingDegree(String string) {
        FList<TextRange> ranges = myCaseInsensitiveMatcher.matchingFragments(string);
        if (ranges != null && !ranges.isEmpty()) {
            int matchStart = ranges.get(0).getStartOffset();
            int underscoreEnd = skipUnderscores(string);
            if (matchStart > 0 && matchStart <= underscoreEnd) {
                return myCaseInsensitiveMatcher.matchingDegree(string.substring(matchStart), true) - 1;
            }
        }

        return myMatcher.matchingDegree(string, true);
    }
}
