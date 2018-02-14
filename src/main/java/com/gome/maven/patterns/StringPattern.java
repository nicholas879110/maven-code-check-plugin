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

import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.ProcessingContext;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.DatatypesAutomatonProvider;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * @author peter
 */
public class StringPattern extends ObjectPattern<String, StringPattern> {
    private static final InitialPatternCondition<String> CONDITION = new InitialPatternCondition<String>(String.class) {
        @Override
        public boolean accepts( final Object o, final ProcessingContext context) {
            return o instanceof String;
        }


        @Override
        public void append(  final StringBuilder builder, final String indent) {
            builder.append("string()");
        }
    };

    protected StringPattern() {
        super(CONDITION);
    }

    
    public StringPattern startsWith(  final String s) {
        return with(new PatternCondition<String>("startsWith") {
            @Override
            public boolean accepts( final String str, final ProcessingContext context) {
                return StringUtil.startsWith(str, s);
            }
        });
    }

    
    public StringPattern endsWith(  final String s) {
        return with(new PatternCondition<String>("endsWith") {
            @Override
            public boolean accepts( final String str, final ProcessingContext context) {
                return StringUtil.endsWith(str, s);
            }
        });
    }

    
    public StringPattern contains(  final String s) {
        return with(new PatternCondition<String>("contains") {
            @Override
            public boolean accepts( final String str, final ProcessingContext context) {
                return StringUtil.contains(str, s);
            }
        });
    }

    
    public StringPattern containsChars(  final String s) {
        return with(new PatternCondition<String>("containsChars") {
            @Override
            public boolean accepts( final String str, final ProcessingContext context) {
                return StringUtil.containsAnyChar(str, s);
            }
        });
    }

    
    public StringPattern matches(  final String s) {
        final String escaped = StringUtil.escapeToRegexp(s);
        if (escaped.equals(s)) {
            return equalTo(s);
        }
        // may throw PatternSyntaxException here
        final Pattern pattern = Pattern.compile(s);
        return with(new ValuePatternCondition<String>("matches") {
            @Override
            public boolean accepts( final String str, final ProcessingContext context) {
                return pattern.matcher(newBombedCharSequence(str)).matches();
            }

            @Override
            public Collection<String> getValues() {
                return Collections.singleton(s);
            }
        });
    }

    
    public StringPattern matchesBrics(  final String s) {
        final String escaped = StringUtil.escapeToRegexp(s);
        if (escaped.equals(s)) {
            return equalTo(s);
        }

        StringBuilder sb = new StringBuilder(s.length()*5);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if(c == ' ') {
                sb.append("<whitespacechar>");
            }
            else
                //This is really stupid and inconvenient builder - it breaks any normal pattern with uppercase
                if(Character.isUpperCase(c)) {
                    sb.append('[').append(Character.toUpperCase(c)).append(Character.toLowerCase(c)).append(']');
                }
                else
                {
                    sb.append(c);
                }
        }
        final RegExp regExp = new RegExp(sb.toString());
        final Automaton automaton = regExp.toAutomaton(new DatatypesAutomatonProvider());
        final RunAutomaton runAutomaton = new RunAutomaton(automaton, true);

        return with(new ValuePatternCondition<String>("matchesBrics") {
            @Override
            public boolean accepts( String str, final ProcessingContext context) {
                if (!str.isEmpty() && (str.charAt(0) == '"' || str.charAt(0) == '\'')) str = str.substring(1);
                return runAutomaton.run(str);
            }

            @Override
            public Collection<String> getValues() {
                return Collections.singleton(s);
            }
        });
    }

    
    public StringPattern contains(  final ElementPattern<Character> pattern) {
        return with(new PatternCondition<String>("contains") {
            @Override
            public boolean accepts( final String str, final ProcessingContext context) {
                for (int i = 0; i < str.length(); i++) {
                    if (pattern.accepts(str.charAt(i))) return true;
                }
                return false;
            }
        });
    }

    public StringPattern longerThan(final int minLength) {
        return with(new PatternCondition<String>("longerThan") {
            @Override
            public boolean accepts( final String s, final ProcessingContext context) {
                return s.length() > minLength;
            }
        });
    }

    public StringPattern shorterThan(final int maxLength) {
        return with(new PatternCondition<String>("shorterThan") {
            @Override
            public boolean accepts( final String s, final ProcessingContext context) {
                return s.length() < maxLength;
            }
        });
    }

    public StringPattern withLength(final int length) {
        return with(new PatternCondition<String>("withLength") {
            @Override
            public boolean accepts( final String s, final ProcessingContext context) {
                return s.length() == length;
            }
        });
    }

    @Override
    
    public StringPattern oneOf( final String... values) {
        return super.oneOf(values);
    }

    
    public StringPattern oneOfIgnoreCase( final String... values) {
        return with(new CaseInsensitiveValuePatternCondition("oneOfIgnoreCase", values));
    }

    @Override
    
    public StringPattern oneOf( final Collection<String> set) {
        return super.oneOf(set);
    }

    
    public static CharSequence newBombedCharSequence( CharSequence sequence) {
        return new StringUtil.BombedCharSequence(sequence) {
            @Override
            protected void checkCanceled() {
                ProgressManager.checkCanceled();
            }
        };
    }
}
