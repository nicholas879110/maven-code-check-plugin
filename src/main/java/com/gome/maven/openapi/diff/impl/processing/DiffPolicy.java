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
package com.gome.maven.openapi.diff.impl.processing;

import com.gome.maven.openapi.diff.impl.string.DiffString;
import com.gome.maven.openapi.diff.ex.DiffFragment;
import com.gome.maven.openapi.diff.impl.ComparisonPolicy;
import com.gome.maven.util.diff.FilesTooBigForDiffException;

public interface DiffPolicy {
    
    DiffFragment[] buildFragments( DiffString text1,  DiffString text2) throws FilesTooBigForDiffException;

    
    
    DiffFragment[] buildFragments( String text1,  String text2) throws FilesTooBigForDiffException;

    DiffPolicy LINES_WO_FORMATTING = new LineBlocks(ComparisonPolicy.IGNORE_SPACE);
    DiffPolicy DEFAULT_LINES = new LineBlocks(ComparisonPolicy.DEFAULT);

    class LineBlocks implements DiffPolicy {
        private final ComparisonPolicy myComparisonPolicy;

        public LineBlocks(ComparisonPolicy comparisonPolicy) {
            myComparisonPolicy = comparisonPolicy;
        }

        
        
        public DiffFragment[] buildFragments( String text1,  String text2) throws FilesTooBigForDiffException {
            return buildFragments(DiffString.create(text1), DiffString.create(text2));
        }

        
        @Override
        public DiffFragment[] buildFragments( DiffString text1,  DiffString text2) throws FilesTooBigForDiffException {
            DiffString[] strings1 = text1.tokenize();
            DiffString[] strings2 = text2.tokenize();
            return myComparisonPolicy.buildDiffFragmentsFromLines(strings1, strings2);
        }

    }

    class ByChar implements DiffPolicy {
        private final ComparisonPolicy myComparisonPolicy;

        public ByChar(ComparisonPolicy comparisonPolicy) {
            myComparisonPolicy = comparisonPolicy;
        }

        
        
        public DiffFragment[] buildFragments( String text1,  String text2) throws FilesTooBigForDiffException {
            return buildFragments(DiffString.create(text1), DiffString.create(text2));
        }

        
        @Override
        public DiffFragment[] buildFragments( DiffString text1,  DiffString text2) throws FilesTooBigForDiffException {
            return myComparisonPolicy.buildFragments(splitByChar(text1), splitByChar(text2));
        }

        private static DiffString[] splitByChar( DiffString text) {
            DiffString[] result = new DiffString[text.length()];
            for (int i = 0; i < result.length; i++) {
                result[i] = text.substring(i, i + 1);
            }
            return result;
        }
    }

}
