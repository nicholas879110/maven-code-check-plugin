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
package com.gome.maven.diff;

import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.requests.ContentDiffRequest;
import com.gome.maven.diff.requests.SimpleDiffRequest;
import com.gome.maven.openapi.diff.DiffBundle;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vfs.VirtualFile;

public class DiffRequestFactoryImpl extends DiffRequestFactory {
    private DiffContentFactory myContentFactory = DiffContentFactory.getInstance();

    @Override
    
    public ContentDiffRequest createFromFiles( Project project,  VirtualFile file1,  VirtualFile file2) {
        DiffContent content1 = myContentFactory.create(project, file1);
        DiffContent content2 = myContentFactory.create(project, file2);

        String title1 = getContentTitle(file1);
        String title2 = getContentTitle(file2);

        String title = getTitle(file1, file2);

        return new SimpleDiffRequest(title, content1, content2, title1, title2);
    }

    @Override
    
    public ContentDiffRequest createClipboardVsValue( String value) {
        DiffContent content1 = myContentFactory.createClipboardContent();
        DiffContent content2 = myContentFactory.create(value);

        String title1 = DiffBundle.message("diff.content.clipboard.content.title");
        String title2 = DiffBundle.message("diff.content.selected.value");

        String title = DiffBundle.message("diff.clipboard.vs.value.dialog.title");

        return new SimpleDiffRequest(title, content1, content2, title1, title2);
    }

    @Override
    
    public String getContentTitle( VirtualFile file) {
        if (file.isDirectory()) return file.getPath();

        VirtualFile parent = file.getParent();
        return getContentTitle(file.getName(), file.getPath(), parent != null ? parent.getPath() : null);
    }

    @Override
    
    public String getTitle( VirtualFile file1,  VirtualFile file2) {
        if ((file1.isDirectory() || file2.isDirectory()) && file1.getPath().equals(file2.getPath())) return file1.getPath();
        if (file1.isDirectory() ^ file2.isDirectory()) return getContentTitle(file1) + " vs " + getContentTitle(file2);

        VirtualFile parent1 = file1.getParent();
        VirtualFile parent2 = file2.getParent();
        return getRequestTitle(file1.getName(), file1.getPath(), parent1 != null ? parent1.getPath() : null,
                file2.getName(), file2.getPath(), parent2 != null ? parent2.getPath() : null,
                " vs ");
    }

    @Override
    
    public String getTitle( VirtualFile file) {
        return getTitle(file, file);
    }

    
    public static String getContentTitle( FilePath path) {
        if (path.isDirectory()) return path.getPath();
        FilePath parent = path.getParentPath();
        return getContentTitle(path.getName(), path.getPath(), parent != null ? parent.getPath() : null);
    }

    
    public static String getTitle( FilePath path1,  FilePath path2,  String separator) {
        if ((path1.isDirectory() || path2.isDirectory()) && path1.getPath().equals(path2.getPath())) return path1.getPath();
        if (path1.isDirectory() ^ path2.isDirectory()) return getContentTitle(path1) + " vs " + getContentTitle(path2);

        FilePath parent1 = path1.getParentPath();
        FilePath parent2 = path2.getParentPath();
        return getRequestTitle(path1.getName(), path1.getPath(), parent1 != null ? parent1.getPath() : null,
                path2.getName(), path2.getPath(), parent2 != null ? parent2.getPath() : null,
                separator);
    }

    
    private static String getContentTitle( String name,  String path,  String parentPath) {
        if (parentPath != null) {
            return name + " (" + parentPath + ")";
        }
        else {
            return path;
        }
    }

    
    private static String getRequestTitle( String name1,  String path1,  String parentPath1,
                                           String name2,  String path2,  String parentPath2,
                                           String sep) {
        if (path1.equals(path2)) return getContentTitle(name1, path1, parentPath1);

        if (Comparing.equal(parentPath1, parentPath2)) {
            if (parentPath1 != null) {
                return name1 + sep + name2 + " (" + parentPath1 + ")";
            }
            else {
                return path1 + sep + path2;
            }
        }
        else {
            if (name1.equals(name2)) {
                if (parentPath1 != null && parentPath2 != null) {
                    return name1 + " (" + parentPath1 + sep + parentPath2 + ")";
                }
                else {
                    return path1 + sep + path2;
                }
            }
            else {
                if (parentPath1 != null && parentPath2 != null) {
                    return name1 + sep + name2 + " (" + parentPath1 + sep + parentPath2 + ")";
                }
                else {
                    return path1 + sep + path2;
                }
            }
        }
    }
}
