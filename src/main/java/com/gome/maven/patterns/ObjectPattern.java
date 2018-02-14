/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.patterns;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.util.InstanceofCheckerGenerator;
import com.gome.maven.util.PairProcessor;
import com.gome.maven.util.ProcessingContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author peter
 */
public abstract class ObjectPattern<T, Self extends ObjectPattern<T, Self>> implements Cloneable, ElementPattern<T> {
    private ElementPatternCondition<T> myCondition;

    protected ObjectPattern( final InitialPatternCondition<T> condition) {
        myCondition = new ElementPatternCondition<T>(condition);
    }

    protected ObjectPattern(final Class<T> aClass) {
        final Condition<Object> checker = InstanceofCheckerGenerator.getInstance().getInstanceofChecker(aClass);
        myCondition = new ElementPatternCondition<T>(new InitialPatternCondition<T>(aClass) {
            public boolean accepts( final Object o, final ProcessingContext context) {
                return checker.value(o);
            }
        });
    }

    public final boolean accepts( Object t) {
        return myCondition.accepts(t, new ProcessingContext());
    }

    public boolean accepts( final Object o, final ProcessingContext context) {
        return myCondition.accepts(o, context);
    }

    public final ElementPatternCondition getCondition() {
        return myCondition;
    }

    public Self andNot(final ElementPattern pattern) {
        ElementPattern<T> not = StandardPatterns.not(pattern);
        return and(not);
    }

    public Self andOr( ElementPattern... patterns) {
        ElementPattern or = StandardPatterns.or(patterns);
        return and(or);
    }

    public Self and(final ElementPattern pattern) {
        return with(new PatternConditionPlus<T, T>("and", pattern) {
            @Override
            public boolean processValues(T t, ProcessingContext context, PairProcessor<T, ProcessingContext> processor) {
                return processor.process(t, context);
            }
        });
    }

    public Self equalTo( final T o) {
        return with(new ValuePatternCondition<T>("equalTo") {
            public boolean accepts( final T t, final ProcessingContext context) {
                return t.equals(o);
            }

            @Override
            public Collection<T> getValues() {
                return Collections.singletonList(o);
            }
        });
    }

    
    public Self oneOf(final T... values) {
        final Collection<T> list;

        final int length = values.length;
        if (length == 1) {
            list = Collections.singletonList(values[0]);
        }
        else if (length >= 11) {
            list = new HashSet<T>(Arrays.asList(values));
        }
        else {
            list = Arrays.asList(values);
        }

        return with(new ValuePatternCondition<T>("oneOf") {

            @Override
            public Collection<T> getValues() {
                return list;
            }

            @Override
            public boolean accepts( T t, ProcessingContext context) {
                return list.contains(t);
            }
        });
    }

    
    public Self oneOf(final Collection<T> set) {
        return with(new ValuePatternCondition<T>("oneOf") {

            @Override
            public Collection<T> getValues() {
                return set;
            }

            @Override
            public boolean accepts( T t, ProcessingContext context) {
                return set.contains(t);
            }
        });
    }

    public Self isNull() {
        return adapt(new ElementPatternCondition<T>(new InitialPatternCondition(Object.class) {
            public boolean accepts( final Object o, final ProcessingContext context) {
                return o == null;
            }
        }));
    }

    public Self notNull() {
        return adapt(new ElementPatternCondition<T>(new InitialPatternCondition(Object.class) {
            public boolean accepts( final Object o, final ProcessingContext context) {
                return o != null;
            }
        }));
    }

    public Self save(final Key<? super T> key) {
        return with(new PatternCondition<T>("save") {
            public boolean accepts( final T t, final ProcessingContext context) {
                context.put((Key)key, t);
                return true;
            }
        });
    }

    public Self save( final String key) {
        return with(new PatternCondition<T>("save") {
            public boolean accepts( final T t, final ProcessingContext context) {
                context.put(key, t);
                return true;
            }
        });
    }

    public Self with(final PatternCondition<? super T> pattern) {
        final ElementPatternCondition<T> condition = myCondition.append(pattern);
        return adapt(condition);
    }

    private Self adapt(final ElementPatternCondition<T> condition) {
        try {
            final ObjectPattern s = (ObjectPattern)clone();
            s.myCondition = condition;
            return (Self)s;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public Self without(final PatternCondition<? super T> pattern) {
        return with(new PatternCondition<T>("without") {
            public boolean accepts( final T o, final ProcessingContext context) {
                return !pattern.accepts(o, context);
            }
        });
    }

    public String toString() {
        return myCondition.toString();
    }

    public static class Capture<T> extends ObjectPattern<T,Capture<T>> {

        public Capture(final Class<T> aClass) {
            super(aClass);
        }

        public Capture( final InitialPatternCondition<T> condition) {
            super(condition);
        }
    }

}
