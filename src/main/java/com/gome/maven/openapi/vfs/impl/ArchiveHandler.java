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
package com.gome.maven.openapi.vfs.impl;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.io.FileAttributes;
import com.gome.maven.openapi.util.io.FileSystemUtil;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.ArrayUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ArchiveHandler {
    public static final long DEFAULT_LENGTH = 0L;
    public static final long DEFAULT_TIMESTAMP = -1L;

    protected static class EntryInfo {
        public final EntryInfo parent;
        public final String shortName;
        public final boolean isDirectory;
        public final long length;
        public final long timestamp;

        public EntryInfo(EntryInfo parent,  String shortName, boolean isDirectory, long length, long timestamp) {
            this.parent = parent;
            this.shortName = shortName;
            this.isDirectory = isDirectory;
            this.length = length;
            this.timestamp = timestamp;
        }
    }

    private final String myPath;
    private final Object myLock = new Object();
    private volatile Reference<Map<String, EntryInfo>> myEntries = new SoftReference<Map<String, EntryInfo>>(null);
    private boolean myCorrupted = false;

    protected ArchiveHandler( String path) {
        myPath = path;
    }

    
    public File getFile() {
        return new File(myPath);
    }

    
    public FileAttributes getAttributes( String relativePath) {
        if (relativePath.isEmpty()) {
            FileAttributes attributes = FileSystemUtil.getAttributes(myPath);
            return attributes != null ? new FileAttributes(true, false, false, false, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, false) : null;
        }
        else {
            EntryInfo entry = getEntryInfo(relativePath);
            return entry != null ? new FileAttributes(entry.isDirectory, false, false, false, entry.length, entry.timestamp, false) : null;
        }
    }

    
    public String[] list( String relativePath) {
        EntryInfo entry = getEntryInfo(relativePath);
        if (entry == null || !entry.isDirectory) return ArrayUtil.EMPTY_STRING_ARRAY;

        Set<String> names = new HashSet<String>();
        for (EntryInfo info : getEntriesMap().values()) {
            if (info.parent == entry) {
                names.add(info.shortName);
            }
        }
        return ArrayUtil.toStringArray(names);
    }

    
    protected EntryInfo getEntryInfo( String relativePath) {
        return getEntriesMap().get(relativePath);
    }

    
    protected Map<String, EntryInfo> getEntriesMap() {
        Map<String, EntryInfo> map = SoftReference.dereference(myEntries);
        if (map == null) {
            synchronized (myLock) {
                map = SoftReference.dereference(myEntries);

                if (map == null) {
                    if (myCorrupted) {
                        map = Collections.emptyMap();
                    }
                    else {
                        try {
                            map = Collections.unmodifiableMap(createEntriesMap());
                        }
                        catch (Exception e) {
                            myCorrupted = true;
                            Logger.getInstance(getClass()).warn(e.getMessage() + ": " + myPath, e);
                            map = Collections.emptyMap();
                        }
                    }

                    myEntries = new SoftReference<Map<String, EntryInfo>>(map);
                }
            }
        }
        return map;
    }

    
    protected abstract Map<String, EntryInfo> createEntriesMap() throws IOException;

    
    protected EntryInfo createRootEntry() {
        return new EntryInfo(null, "", true, DEFAULT_LENGTH, DEFAULT_TIMESTAMP);
    }

    
    protected EntryInfo getOrCreate( Map<String, EntryInfo> map,  String entryName) {
        EntryInfo entry = map.get(entryName);
        if (entry == null) {
            Pair<String, String> path = splitPath(entryName);
            EntryInfo parentEntry = getOrCreate(map, path.first);
            entry = new EntryInfo(parentEntry, path.second, true, DEFAULT_LENGTH, DEFAULT_TIMESTAMP);
            map.put(entryName, entry);
        }
        return entry;
    }

    
    protected Pair<String, String> splitPath( String entryName) {
        int p = entryName.lastIndexOf('/');
        String parentName = p > 0 ? entryName.substring(0, p) : "";
        String shortName = p > 0 ? entryName.substring(p + 1) : entryName;
        return Pair.create(parentName, shortName);
    }

    
    public abstract byte[] contentsToByteArray( String relativePath) throws IOException;
}