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
package com.gome.maven.util.io;


import java.io.File;
import java.io.IOException;

public class PersistentStringEnumerator extends PersistentEnumeratorDelegate<String> implements AbstractStringEnumerator {
     private final CachingEnumerator<String> myCache;

    public PersistentStringEnumerator( final File file) throws IOException {
        this(file, null);
    }

    public PersistentStringEnumerator( final File file,  PagedFileStorage.StorageLockContext storageLockContext) throws IOException {
        this(file, 1024 * 4, storageLockContext);
    }

    public PersistentStringEnumerator( final File file, boolean cacheLastMappings) throws IOException {
        this(file, 1024 * 4, cacheLastMappings, null);
    }

    public PersistentStringEnumerator( final File file, final int initialSize) throws IOException {
        this(file, initialSize, null);
    }

    public PersistentStringEnumerator( final File file,
                                      final int initialSize,
                                       PagedFileStorage.StorageLockContext lockContext) throws IOException {
        this(file, initialSize, false, lockContext);
    }

    private PersistentStringEnumerator( final File file,
                                       final int initialSize,
                                       boolean cacheLastMappings,
                                        PagedFileStorage.StorageLockContext lockContext) throws IOException {
        super(file, EnumeratorStringDescriptor.INSTANCE, initialSize, lockContext);
        myCache = cacheLastMappings ? new CachingEnumerator<String>(new DataEnumerator<String>() {
            @Override
            public int enumerate( String value) throws IOException {
                return PersistentStringEnumerator.super.enumerate(value);
            }

            
            @Override
            public String valueOf(int idx) throws IOException {
                return PersistentStringEnumerator.super.valueOf(idx);
            }
        }, EnumeratorStringDescriptor.INSTANCE) : null;
    }

    @Override
    public int enumerate( String value) throws IOException {
        return myCache != null ? myCache.enumerate(value) : super.enumerate(value);
    }

    
    @Override
    public String valueOf(int idx) throws IOException {
        return myCache != null ? myCache.valueOf(idx) : super.valueOf(idx);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (myCache != null) myCache.close();
    }

    @Override
    public void markCorrupted() {
        myEnumerator.markCorrupted();
    }
}
