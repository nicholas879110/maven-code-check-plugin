/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff.comparison;

import com.gome.maven.diff.fragments.DiffFragment;
import com.gome.maven.diff.fragments.LineFragment;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.progress.ProgressIndicator;

import java.util.List;

/**
 * Class for the text comparison
 * CharSequences should to have '\n' as line separator
 * <p/>
 * It's good idea not to compare String due to expensive subSequence() implementation. Try to wrap into CharSequenceSubSequence.
 */
public abstract class ComparisonManager {
    
    public static ComparisonManager getInstance() {
        return ServiceManager.getService(ComparisonManager.class);
    }

    
    public abstract List<LineFragment> compareLines( CharSequence text1,
                                                     CharSequence text2,
                                                     ComparisonPolicy policy,
                                                     ProgressIndicator indicator) throws DiffTooBigException;

    
    public abstract List<LineFragment> compareLinesInner( CharSequence text1,
                                                          CharSequence text2,
                                                          ComparisonPolicy policy,
                                                          ProgressIndicator indicator) throws DiffTooBigException;

    
    public abstract List<LineFragment> compareLinesInner( CharSequence text1,
                                                          CharSequence text2,
                                                          List<LineFragment> lineFragments,
                                                          ComparisonPolicy policy,
                                                          ProgressIndicator indicator) throws DiffTooBigException;

    
    public abstract List<DiffFragment> compareWords( CharSequence text1,
                                                     CharSequence text2,
                                                     ComparisonPolicy policy,
                                                     ProgressIndicator indicator) throws DiffTooBigException;

    
    public abstract List<DiffFragment> compareChars( CharSequence text1,
                                                     CharSequence text2,
                                                     ComparisonPolicy policy,
                                                     ProgressIndicator indicator) throws DiffTooBigException;

    public abstract boolean isEquals( CharSequence text1,  CharSequence text2,  ComparisonPolicy policy);

    //
    // Post process line fragments
    //

    
    public abstract List<LineFragment> squash( List<LineFragment> oldFragments);

    
    public abstract List<LineFragment> processBlocks( List<LineFragment> oldFragments,
                                                      final CharSequence text1,  final CharSequence text2,
                                                      final ComparisonPolicy policy,
                                                     final boolean squash, final boolean trim);
}
