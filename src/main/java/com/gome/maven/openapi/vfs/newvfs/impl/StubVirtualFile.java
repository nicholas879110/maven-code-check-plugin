//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.vfs.newvfs.impl;

import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StubVirtualFile extends VirtualFile {
    public StubVirtualFile() {
    }

    
    public byte[] contentsToByteArray() throws IOException {
        throw new UnsupportedOperationException("contentsToByteArray is not implemented");
    }

    public VirtualFile[] getChildren() {
        throw new UnsupportedOperationException("getChildren is not implemented");
    }

    
    public VirtualFileSystem getFileSystem() {
        throw new UnsupportedOperationException("getFileSystem is not implemented");
    }

    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("getInputStream is not implemented");
    }

    public long getLength() {
        throw new UnsupportedOperationException("getLength is not implemented");
    }

    
    
    public String getName() {
        throw new UnsupportedOperationException("getName is not implemented");
    }

    
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException("getOutputStream is not implemented");
    }

    public VirtualFile getParent() {
        throw new UnsupportedOperationException("getParent is not implemented");
    }

    
    public String getPath() {
        throw new UnsupportedOperationException("getPath is not implemented");
    }

    public long getTimeStamp() {
        throw new UnsupportedOperationException("getTimeStamp is not implemented");
    }

    
    public String getUrl() {
        throw new UnsupportedOperationException("getUrl is not implemented");
    }

    public boolean isDirectory() {
        throw new UnsupportedOperationException("isDirectory is not implemented");
    }

    public boolean isValid() {
        throw new UnsupportedOperationException("isValid is not implemented");
    }

    public boolean isWritable() {
        throw new UnsupportedOperationException("isWritable is not implemented");
    }

    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
        throw new UnsupportedOperationException("refresh is not implemented");
    }
}
