package com.gome.maven.openapi.project;

import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.InternalFileType;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.SystemInfoRt;
import com.gome.maven.openapi.vfs.VirtualFile;

/**
 * Author: dmitrylomov
 */
public class ProjectCoreUtil {
    public static final String DIRECTORY_BASED_PROJECT_DIR = ".idea";

    public static boolean isProjectOrWorkspaceFile(final VirtualFile file) {
        return isProjectOrWorkspaceFile(file, file.getFileType());
    }

    public static boolean isProjectOrWorkspaceFile(final VirtualFile file,
                                                   final  FileType fileType) {
        if (fileType instanceof InternalFileType) return true;
        VirtualFile parent = file.isDirectory() ? file: file.getParent();
        while (parent != null) {
            if (Comparing.equal(parent.getNameSequence(), DIRECTORY_BASED_PROJECT_DIR, SystemInfoRt.isFileSystemCaseSensitive)) return true;
            parent = parent.getParent();
        }
        return false;
    }
}
