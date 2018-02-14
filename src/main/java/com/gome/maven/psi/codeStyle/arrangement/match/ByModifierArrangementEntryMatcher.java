/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.psi.codeStyle.arrangement.match;

import com.gome.maven.psi.codeStyle.arrangement.ArrangementEntry;
import com.gome.maven.psi.codeStyle.arrangement.ModifierAwareArrangementEntry;
import com.gome.maven.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.gome.maven.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.gome.maven.util.containers.ContainerUtilRt;

import java.util.Collection;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 8/26/12 11:21 PM
 */
public class ByModifierArrangementEntryMatcher implements ArrangementEntryMatcher {

     private final Set<ArrangementAtomMatchCondition> myModifiers = ContainerUtilRt.newHashSet();

    public ByModifierArrangementEntryMatcher( ArrangementAtomMatchCondition interestedModifier) {
        myModifiers.add(interestedModifier);
    }

    public ByModifierArrangementEntryMatcher( Collection<ArrangementAtomMatchCondition> interestedModifiers) {
        myModifiers.addAll(interestedModifiers);
    }

    @Override
    public boolean isMatched( ArrangementEntry entry) {
        if (entry instanceof ModifierAwareArrangementEntry) {
            final Set<ArrangementSettingsToken> modifiers = ((ModifierAwareArrangementEntry)entry).getModifiers();
            for (ArrangementAtomMatchCondition condition : myModifiers) {
                final Object value = condition.getValue();
                boolean isInverted = value instanceof Boolean && !((Boolean)value);
                if (isInverted == modifiers.contains(condition.getType())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return myModifiers.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ByModifierArrangementEntryMatcher matcher = (ByModifierArrangementEntryMatcher)o;

        if (!myModifiers.equals(matcher.myModifiers)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "with modifiers " + myModifiers;
    }
}
