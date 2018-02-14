/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.psi.codeStyle.arrangement.std;

import com.gome.maven.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.gome.maven.psi.codeStyle.arrangement.ArrangementUtil;
import com.gome.maven.psi.codeStyle.arrangement.match.ArrangementEntryMatcher;
import com.gome.maven.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.gome.maven.ui.SimpleColoredComponent;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.ContainerUtilRt;
import gnu.trove.TObjectIntHashMap;

import java.util.*;

/**
 * Wraps {@link ArrangementStandardSettingsAware} for the common arrangement UI managing code.
 *
 * @author Denis Zhdanov
 * @since 3/7/13 3:11 PM
 */
public class ArrangementStandardSettingsManager {

     private final TObjectIntHashMap<ArrangementSettingsToken> myWidths  = new TObjectIntHashMap<ArrangementSettingsToken>();
     private final TObjectIntHashMap<ArrangementSettingsToken> myWeights = new TObjectIntHashMap<ArrangementSettingsToken>();

     private final Comparator<ArrangementSettingsToken> myComparator = new Comparator<ArrangementSettingsToken>() {
        @Override
        public int compare(ArrangementSettingsToken t1, ArrangementSettingsToken t2) {
            if (myWeights.containsKey(t1)) {
                if (myWeights.containsKey(t2)) {
                    return myWeights.get(t1) - myWeights.get(t2);
                }
                else {
                    return -1;
                }
            }
            else if (myWeights.containsKey(t2)) {
                return 1;
            }
            else {
                return t1.compareTo(t2);
            }
        }
    };

     private final ArrangementStandardSettingsAware          myDelegate;
     private final ArrangementColorsProvider                 myColorsProvider;
     private final Collection<Set<ArrangementSettingsToken>> myMutexes;

     private final StdArrangementSettings                  myDefaultSettings;
     private final List<CompositeArrangementSettingsToken> myGroupingTokens;
     private final List<CompositeArrangementSettingsToken> myMatchingTokens;

     private Collection<StdArrangementRuleAliasToken> myRuleAliases;
     private Set<ArrangementSettingsToken> myRuleAliasMutex;
     private CompositeArrangementSettingsToken myRuleAliasToken;

    public ArrangementStandardSettingsManager( ArrangementStandardSettingsAware delegate,
                                               ArrangementColorsProvider colorsProvider) {
        this(delegate, colorsProvider, ContainerUtil.<StdArrangementRuleAliasToken>emptyList());
    }

    public ArrangementStandardSettingsManager( ArrangementStandardSettingsAware delegate,
                                               ArrangementColorsProvider colorsProvider,
                                               Collection<StdArrangementRuleAliasToken> aliases)
    {
        myDelegate = delegate;
        myColorsProvider = colorsProvider;
        myMutexes = delegate.getMutexes();
        myDefaultSettings = delegate.getDefaultSettings();

        SimpleColoredComponent renderer = new SimpleColoredComponent();
        myGroupingTokens = delegate.getSupportedGroupingTokens();
        if (myGroupingTokens != null) {
            parseWidths(myGroupingTokens, renderer);
            buildWeights(myGroupingTokens);
        }

        myMatchingTokens = delegate.getSupportedMatchingTokens();
        if (myMatchingTokens != null) {
            parseWidths(myMatchingTokens, renderer);
            buildWeights(myMatchingTokens);
        }

        final Set<ArrangementSettingsToken> aliasTokens = ContainerUtil.newHashSet();
        aliasTokens.addAll(aliases);

        myRuleAliases = aliases;
        myRuleAliasMutex = aliasTokens;
        if (!myRuleAliases.isEmpty()) {
            myRuleAliasToken = new CompositeArrangementSettingsToken(StdArrangementTokens.General.ALIAS, aliasTokens);
        }
    }

    
    public Collection<StdArrangementRuleAliasToken> getRuleAliases() {
        return myRuleAliases;
    }

    
    public ArrangementStandardSettingsAware getDelegate() {
        return myDelegate;
    }

    private void parseWidths( Collection<CompositeArrangementSettingsToken> compositeTokens,
                              SimpleColoredComponent renderer)
    {
        int width = 0;
        for (CompositeArrangementSettingsToken compositeToken : compositeTokens) {
            width = Math.max(width, parseWidth(compositeToken.getToken(), renderer));
        }
        for (CompositeArrangementSettingsToken compositeToken : compositeTokens) {
            myWidths.put(compositeToken.getToken(), width);
            parseWidths(compositeToken.getChildren(), renderer);
        }
    }

    private void buildWeights( Collection<CompositeArrangementSettingsToken> compositeTokens) {
        for (CompositeArrangementSettingsToken token : compositeTokens) {
            myWeights.put(token.getToken(), myWeights.size());
            buildWeights(token.getChildren());
        }
    }

    /**
     * @see ArrangementStandardSettingsAware#getDefaultSettings()
     */
    
    public StdArrangementSettings getDefaultSettings() {
        return myDefaultSettings;
    }

    public boolean isSectionRulesSupported() {
        return myDelegate instanceof ArrangementSectionRuleAwareSettings;
    }

    /**
     * @see ArrangementStandardSettingsAware#getSupportedGroupingTokens()
     */
    
    public List<CompositeArrangementSettingsToken> getSupportedGroupingTokens() {
        return myGroupingTokens;
    }

    /**
     * @see ArrangementStandardSettingsAware#getSupportedMatchingTokens()
     */
    
    public List<CompositeArrangementSettingsToken> getSupportedMatchingTokens() {
        if (myMatchingTokens == null || myRuleAliasToken == null) {
            return myMatchingTokens;
        }

        final List<CompositeArrangementSettingsToken> allTokens = ContainerUtil.newArrayList(myMatchingTokens);
        allTokens.add(myRuleAliasToken);
        return allTokens;
    }

    public boolean isEnabled( ArrangementSettingsToken token,  ArrangementMatchCondition current) {
        if (myRuleAliasMutex.contains(token)) {
            return true;
        }
        return myDelegate.isEnabled(token, current);
    }

    
    public ArrangementEntryMatcher buildMatcher( ArrangementMatchCondition condition) throws IllegalArgumentException {
        ArrangementEntryMatcher matcher = ArrangementUtil.buildMatcher(condition);
        if (matcher == null) {
            matcher = myDelegate.buildMatcher(condition);
        }
        return matcher;
    }

    
    public Collection<Set<ArrangementSettingsToken>> getMutexes() {
        if (myRuleAliasMutex.isEmpty()) {
            return myMutexes;
        }
        final List<Set<ArrangementSettingsToken>> allMutexes = ContainerUtil.newArrayList(myMutexes);
        allMutexes.add(myRuleAliasMutex);
        return allMutexes;
    }

    public int getWidth( ArrangementSettingsToken token) {
        if (myWidths.containsKey(token)) {
            return myWidths.get(token);
        }
        return parseWidth(token, new SimpleColoredComponent());
    }

    private int parseWidth( ArrangementSettingsToken token,  SimpleColoredComponent renderer) {
        renderer.clear();
        final String value = getPresentationValue(token);
        renderer.append(value, SimpleTextAttributes.fromTextAttributes(myColorsProvider.getTextAttributes(token, true)));
        int result = renderer.getPreferredSize().width;

        renderer.clear();
        renderer.append(value, SimpleTextAttributes.fromTextAttributes(myColorsProvider.getTextAttributes(token, false)));
        return Math.max(result, renderer.getPreferredSize().width);
    }

    
    private static String getPresentationValue( ArrangementSettingsToken token) {
        if (token instanceof InvertibleArrangementSettingsToken) {
            return ((InvertibleArrangementSettingsToken)token).getInvertedRepresentationValue();
        }
        return token.getRepresentationValue();
    }

    public List<ArrangementSettingsToken> sort( Collection<ArrangementSettingsToken> tokens) {
        List<ArrangementSettingsToken> result = ContainerUtilRt.newArrayList(tokens);
        Collections.sort(result, myComparator);
        return result;
    }
}
