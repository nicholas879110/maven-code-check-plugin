//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.util.indexing;

import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.UserDataHolder;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiFile;

public interface FileContent extends UserDataHolder {
    
    FileType getFileType();

    
    VirtualFile getFile();

    
    String getFileName();

    byte[] getContent();

    
    CharSequence getContentAsText();

    Project getProject();

    
    PsiFile getPsiFile();
}
