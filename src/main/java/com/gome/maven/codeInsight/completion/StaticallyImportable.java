package com.gome.maven.codeInsight.completion;

import com.gome.maven.openapi.util.ClassConditionKey;

/**
 * @author peter
 */
public interface StaticallyImportable {
    ClassConditionKey<StaticallyImportable> CLASS_CONDITION_KEY = ClassConditionKey.create(StaticallyImportable.class);

    void setShouldBeImported(boolean shouldImportStatic);

    boolean canBeImported();

    boolean willBeImported();
}
