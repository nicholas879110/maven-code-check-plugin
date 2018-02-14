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
package com.gome.maven.psi.codeStyle.arrangement.model;

import com.gome.maven.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.gome.maven.psi.codeStyle.arrangement.std.InvertibleArrangementSettingsToken;

/**
 * Encapsulates a single atom match condition, e.g. 'entry type is field' or 'entry has 'static' modifier' etc.
 * <p/>
 * Not thread-safe..
 *
 * @author Denis Zhdanov
 * @since 8/8/12 1:17 PM
 */
public class ArrangementAtomMatchCondition implements ArrangementMatchCondition {

     private final ArrangementSettingsToken myType;
     private final Object                   myValue;

    public ArrangementAtomMatchCondition( ArrangementSettingsToken type) {
        this(type, type instanceof InvertibleArrangementSettingsToken ? Boolean.TRUE : type);
    }

    public ArrangementAtomMatchCondition( ArrangementSettingsToken type,  Object value) {
        myType = type;
        myValue = value;
    }

    
    public ArrangementSettingsToken getType() {
        return myType;
    }

    
    public Object getValue() {
        return myValue;
    }

    @Override
    public void invite( ArrangementMatchConditionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int hashCode() {
        int result = myType.hashCode();
        result = 31 * result + myValue.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrangementAtomMatchCondition setting = (ArrangementAtomMatchCondition)o;

        if (!myType.equals(setting.myType)) {
            return false;
        }
        if (!myValue.equals(setting.myValue)) {
            return false;
        }

        return true;
    }

    
    @Override
    public ArrangementAtomMatchCondition clone() {
        return new ArrangementAtomMatchCondition(myType, myValue);
    }

    @Override
    public String toString() {
        if (myValue instanceof Boolean) {
            return String.format("%s%s", (Boolean)myValue ? "" : "not " , myType.getRepresentationValue());
        }
        else {
            return String.format("%s: %s", myType.getRepresentationValue(), myValue.toString().toLowerCase());
        }
    }
}
