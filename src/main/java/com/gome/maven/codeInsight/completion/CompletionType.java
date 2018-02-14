package com.gome.maven.codeInsight.completion;

/**
 * @author peter
 */
public enum CompletionType {
    BASIC,
    SMART,

    /**
     * Only to be passed to {@link CompletionService#getVariantsFromContributors(CompletionParameters, CompletionContributor, com.gome.maven.util.Consumer)}
     * to invoke special class-name providers for various file types where those class names are applicable (e.g. xml, txt, properties, custom)
     */
    CLASS_NAME
}
