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
package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.psi.Weigher;
import com.gome.maven.util.Consumer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * For completion FAQ, see {@link CompletionContributor}.
 *
 * @author peter
 */
public abstract class CompletionService {
    public static final Key<CompletionStatistician> STATISTICS_KEY = Key.create("completion");
    /**
     * A "weigher" extension key (see {@link Weigher}) to sort completion items by priority and move the heaviest to the top of the Lookup.
     */
    public static final Key<CompletionWeigher> RELEVANCE_KEY = Key.create("completion");
    /**
     * A "weigher" extension key (see {@link Weigher}) to sort the whole lookup descending.
     * @deprecated use "completion" relevance key instead
     */
    public static final Key<CompletionWeigher> SORTING_KEY = Key.create("completionSorting");

    public static CompletionService getCompletionService() {
        return ServiceManager.getService(CompletionService.class);
    }

    /**
     * @return Current lookup advertisement text (at the bottom).
     */
    
    public abstract String getAdvertisementText();

    /**
     * Set lookup advertisement text (at the bottom) at any time. Will do nothing if no completion process is in progress.
     * @param text
     * @deprecated use {@link CompletionResultSet#addLookupAdvertisement(String)}
     */
    public abstract void setAdvertisementText( String text);

    /**
     * Run all contributors until any of them returns false or the list is exhausted. If from parameter is not null, contributors
     * will be run starting from the next one after that.
     * @param parameters
     * @param from
     * @param consumer
     * @return
     */
    public void getVariantsFromContributors(final CompletionParameters parameters,
                                             final CompletionContributor from,
                                            final Consumer<CompletionResult> consumer) {
        final List<CompletionContributor> contributors = CompletionContributor.forParameters(parameters);

        for (int i = contributors.indexOf(from) + 1; i < contributors.size(); i++) {
            final CompletionContributor contributor = contributors.get(i);

            final CompletionResultSet result = createResultSet(parameters, consumer, contributor);
            contributor.fillCompletionVariants(parameters, result);
            if (result.isStopped()) {
                return;
            }
        }
    }

    /**
     * Create a {@link com.gome.maven.codeInsight.completion.CompletionResultSet} that will filter variants based on default camel-hump
     * {@link com.gome.maven.codeInsight.completion.PrefixMatcher} and give the filtered variants to consumer.
     * @param parameters
     * @param consumer
     * @param contributor
     * @return
     */
    public abstract CompletionResultSet createResultSet(CompletionParameters parameters, Consumer<CompletionResult> consumer,
                                                         CompletionContributor contributor);

    
    public abstract CompletionProcess getCurrentCompletion();

    /**
     * The main method that is invoked to collect all the completion variants
     * @param parameters Parameters specifying current completion environment
     * @param consumer This consumer will directly add lookup elements to the lookup
     * @return all suitable lookup elements
     */
    
    public LookupElement[] performCompletion(final CompletionParameters parameters, final Consumer<CompletionResult> consumer) {
        final Collection<LookupElement> lookupSet = new LinkedHashSet<LookupElement>();

        getVariantsFromContributors(parameters, null, new Consumer<CompletionResult>() {
            @Override
            public void consume(final CompletionResult result) {
                if (lookupSet.add(result.getLookupElement())) {
                    consumer.consume(result);
                }
            }
        });
        return lookupSet.toArray(new LookupElement[lookupSet.size()]);
    }

    public abstract CompletionSorter defaultSorter(CompletionParameters parameters, PrefixMatcher matcher);

    public abstract CompletionSorter emptySorter();

}
