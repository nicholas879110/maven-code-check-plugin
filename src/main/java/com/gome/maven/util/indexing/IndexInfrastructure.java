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

/*
 * @author max
 */
package com.gome.maven.util.indexing;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.ex.dummy.DummyFileSystem;
import com.gome.maven.openapi.vfs.newvfs.persistent.PersistentFS;
import com.gome.maven.psi.stubs.StubIndexKey;
import com.gome.maven.psi.stubs.StubUpdatingIndex;

import java.io.File;
import java.util.Locale;

@SuppressWarnings({"HardCodedStringLiteral"})
public class IndexInfrastructure {
    private static final boolean ourUnitTestMode = ApplicationManager.getApplication().isUnitTestMode();
    private static final String STUB_VERSIONS = ".versions";
    private static final String PERSISTENT_INDEX_DIRECTORY_NAME = ".persistent";

    private IndexInfrastructure() {
    }

    
    public static File getVersionFile( ID<?, ?> indexName) {
        return new File(getIndexDirectory(indexName, true), indexName + ".ver");
    }

    
    public static File getStorageFile( ID<?, ?> indexName) {
        return new File(getIndexRootDir(indexName), indexName.toString());
    }

    
    public static File getInputIndexStorageFile( ID<?, ?> indexName) {
        return new File(getIndexRootDir(indexName), indexName +"_inputs");
    }

    
    public static File getIndexRootDir( ID<?, ?> indexName) {
        return getIndexDirectory(indexName, false);
    }

    public static File getPersistentIndexRoot() {
        File indexDir = new File(PathManager.getIndexRoot() + File.separator + PERSISTENT_INDEX_DIRECTORY_NAME);
        indexDir.mkdirs();
        return indexDir;
    }

    
    public static File getPersistentIndexRootDir( ID<?, ?> indexName) {
        return getIndexDirectory(indexName, false, PERSISTENT_INDEX_DIRECTORY_NAME);
    }

    
    private static File getIndexDirectory( ID<?, ?> indexName, boolean forVersion) {
        return getIndexDirectory(indexName, forVersion, "");
    }

    
    private static File getIndexDirectory( ID<?, ?> indexName, boolean forVersion, String relativePath) {
        final String dirName = indexName.toString().toLowerCase(Locale.US);
        File indexDir;

        if (indexName instanceof StubIndexKey) {
            // store StubIndices under StubUpdating index' root to ensure they are deleted
            // when StubUpdatingIndex version is changed
            indexDir = new File(getIndexDirectory(StubUpdatingIndex.INDEX_ID, false, relativePath), forVersion ? STUB_VERSIONS : dirName);
        } else {
            if (relativePath.length() > 0) relativePath = File.separator + relativePath;
            indexDir = new File(PathManager.getIndexRoot() + relativePath, dirName);
        }
        indexDir.mkdirs();
        return indexDir;
    }

    
    public static VirtualFile findFileById( PersistentFS fs, final int id) {
        if (ourUnitTestMode) {
            final VirtualFile testFile = findTestFile(id);
            if (testFile != null) {
                return testFile;
            }
        }

        return fs.findFileById(id);

    /*

    final boolean isDirectory = fs.isDirectory(id);
    final DirectoryInfo directoryInfo = isDirectory ? dirIndex.getInfoForDirectoryId(id) : dirIndex.getInfoForDirectoryId(fs.getParent(id));
    if (directoryInfo != null && (directoryInfo.contentRoot != null || directoryInfo.sourceRoot != null || directoryInfo.libraryClassRoot != null)) {
      return isDirectory? directoryInfo.directory : directoryInfo.directory.findChild(fs.getName(id));
    }
    return null;
    */
    }

    
    public static VirtualFile findFileByIdIfCached( PersistentFS fs, final int id) {
        if (ourUnitTestMode) {
            final VirtualFile testFile = findTestFile(id);
            if (testFile != null) {
                return testFile;
            }
        }
        return fs.findFileByIdIfCached(id);
    }

    
    private static VirtualFile findTestFile(final int id) {
        return DummyFileSystem.getInstance().findById(id);
    }
}
