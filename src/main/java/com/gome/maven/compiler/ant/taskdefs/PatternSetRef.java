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

package com.gome.maven.compiler.ant.taskdefs;

import com.gome.maven.compiler.ant.Tag;
import com.gome.maven.openapi.util.Couple;

/**
 * @author Eugene Zhuravlev
 *         Date: Mar 19, 2004
 */
public class PatternSetRef extends Tag{
    public PatternSetRef( final String refid) {
        //noinspection HardCodedStringLiteral
        super("patternset", Couple.of("refid", refid));
    }
}
