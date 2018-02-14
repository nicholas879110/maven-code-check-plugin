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
package com.gome.maven.diff.util;

import com.gome.maven.diff.fragments.DiffFragment;
import com.gome.maven.diff.fragments.LineFragment;
import com.gome.maven.openapi.util.Couple;

import java.util.List;

public enum Side {
    LEFT(0),
    RIGHT(1);

    private final int myIndex;

    Side(int index) {
        myIndex = index;
    }

    
    public static Side fromLeft(boolean isLeft) {
        return isLeft ? LEFT : RIGHT;
    }

    public int getIndex() {
        return myIndex;
    }

    public boolean isLeft() {
        return myIndex == 0;
    }

    
    public Side other() {
        return isLeft() ? RIGHT : LEFT;
    }

    
    public Side other(boolean other) {
        return other ? other() : this;
    }

    //
    // Helpers
    //

    public int select(int left, int right) {
        return isLeft() ? left : right;
    }

    public <T> T select( T left,  T right) {
        return isLeft() ? left : right;
    }

    
    public <T> T selectNotNull( T left,  T right) {
        return isLeft() ? left : right;
    }

    public int select( int[] array) {
        assert array.length == 2;
        return array[myIndex];
    }

    public <T> T select( T[] array) {
        assert array.length == 2;
        return array[myIndex];
    }

    
    public <T> T selectNotNull( T[] array) {
        assert array.length == 2;
        return array[myIndex];
    }

    public <T> T select( List<T> list) {
        assert list.size() == 2;
        return list.get(myIndex);
    }

    
    public <T> T selectNotNull( List<T> list) {
        assert list.size() == 2;
        return list.get(myIndex);
    }

    public <T> T select( Couple<T> region) {
        return isLeft() ? region.first : region.second;
    }

    
    public <T> T selectNotNull( Couple<T> region) {
        return isLeft() ? region.first : region.second;
    }

    //
    // Fragments
    //

    public int getStartOffset( DiffFragment fragment) {
        return isLeft() ? fragment.getStartOffset1() : fragment.getStartOffset2();
    }

    public int getEndOffset( DiffFragment fragment) {
        return isLeft() ? fragment.getEndOffset1() : fragment.getEndOffset2();
    }

    public int getStartLine( LineFragment fragment) {
        return isLeft() ? fragment.getStartLine1() : fragment.getStartLine2();
    }

    public int getEndLine( LineFragment fragment) {
        return isLeft() ? fragment.getEndLine1() : fragment.getEndLine2();
    }
}
