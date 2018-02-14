//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.fileTypes;

import com.gome.maven.ide.highlighter.ArchiveFileType;

public class FileTypes {
    public static final FileType ARCHIVE;
    public static final FileType UNKNOWN;
    public static final LanguageFileType PLAIN_TEXT;

    protected FileTypes() {
    }

    static {
        ARCHIVE = ArchiveFileType.INSTANCE;
        UNKNOWN = UnknownFileType.INSTANCE;
        PLAIN_TEXT = (LanguageFileType)FileTypeManager.getInstance().getStdFileType("PLAIN_TEXT");
    }
}
