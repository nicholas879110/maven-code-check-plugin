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

package com.gome.maven.psi.impl.cache.impl.id;

import com.gome.maven.lang.cacheBuilder.CacheBuilderRegistry;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.search.UsageSearchContext;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.indexing.*;
import com.gome.maven.util.io.DataExternalizer;
import com.gome.maven.util.io.InlineKeyDescriptor;
import com.gome.maven.util.io.KeyDescriptor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 16, 2008
 */
public class IdIndex extends FileBasedIndexExtension<IdIndexEntry, Integer> {
     public static final ID<IdIndexEntry, Integer> NAME = ID.create("IdIndex");

    private final FileBasedIndex.InputFilter myInputFilter = new FileBasedIndex.InputFilter() {
        @Override
        public boolean acceptInput( final VirtualFile file) {
            return isIndexable(file.getFileType());
        }
    };

    public static final boolean ourSnapshotMappingsEnabled = SystemProperties.getBooleanProperty("idea.index.snapshot.mappings.enabled", true);

    private final DataExternalizer<Integer> myValueExternalizer = new DataExternalizer<Integer>() {
        @Override
        public void save( final DataOutput out, final Integer value) throws IOException {
            out.write(value.intValue() & UsageSearchContext.ANY);
        }

        @Override
        public Integer read( final DataInput in) throws IOException {
            return Integer.valueOf(in.readByte() & UsageSearchContext.ANY);
        }
    };

    private final KeyDescriptor<IdIndexEntry> myKeyDescriptor = new InlineKeyDescriptor<IdIndexEntry>() {
        @Override
        public IdIndexEntry fromInt(int n) {
            return new IdIndexEntry(n);
        }

        @Override
        public int toInt(IdIndexEntry idIndexEntry) {
            return idIndexEntry.getWordHashCode();
        }
    };

    private final DataIndexer<IdIndexEntry, Integer, FileContent> myIndexer = new DataIndexer<IdIndexEntry, Integer, FileContent>() {
        @Override
        
        public Map<IdIndexEntry, Integer> map( final FileContent inputData) {
            final FileTypeIdIndexer indexer = IdTableBuilding.getFileTypeIndexer(inputData.getFileType());
            if (indexer != null) {
                return indexer.map(inputData);
            }

            return Collections.emptyMap();
        }
    };

    @Override
    public int getVersion() {
        return 15 + (ourSnapshotMappingsEnabled ? 0xFF:0); // TODO: version should enumerate all word scanner versions and build version upon that set
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    
    @Override
    public ID<IdIndexEntry,Integer> getName() {
        return NAME;
    }

    
    @Override
    public DataIndexer<IdIndexEntry, Integer, FileContent> getIndexer() {
        return myIndexer;
    }

    
    @Override
    public DataExternalizer<Integer> getValueExternalizer() {
        return myValueExternalizer;
    }

    
    @Override
    public KeyDescriptor<IdIndexEntry> getKeyDescriptor() {
        return myKeyDescriptor;
    }

    
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return myInputFilter;
    }

    public static boolean isIndexable(FileType fileType) {
        return fileType instanceof LanguageFileType ||
                fileType instanceof CustomSyntaxTableFileType ||
                IdTableBuilding.isIdIndexerRegistered(fileType) ||
                CacheBuilderRegistry.getInstance().getCacheBuilder(fileType) != null;
    }

    @Override
    public boolean hasSnapshotMapping() {
        return true;
    }
}
