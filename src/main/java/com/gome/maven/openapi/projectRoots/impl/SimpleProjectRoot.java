/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.gome.maven.openapi.projectRoots.impl;

import com.gome.maven.openapi.projectRoots.ex.ProjectRoot;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.util.PathUtil;
import com.gome.maven.util.io.URLUtil;
import org.jdom.Element;

import java.io.File;

/**
 * @author mike
 */
public class SimpleProjectRoot implements ProjectRoot, JDOMExternalizable {
    private String myUrl;
    private VirtualFile myFile;
    private final VirtualFile[] myFileArray = new VirtualFile[1];
    private boolean myInitialized = false;
     private static final String ATTRIBUTE_URL = "url";

    public SimpleProjectRoot( VirtualFile file) {
        myFile = file;
        myUrl = myFile.getUrl();
    }

    public SimpleProjectRoot( String url) {
        myUrl = url;
    }

    SimpleProjectRoot() {
    }

    public VirtualFile getFile() {
        return myFile;
    }

    @Override
    
    public String getPresentableString() {
        String path = VirtualFileManager.extractPath(myUrl);
        if (path.endsWith(URLUtil.JAR_SEPARATOR)) {
            path = path.substring(0, path.length() - URLUtil.JAR_SEPARATOR.length());
        }
        return path.replace('/', File.separatorChar);
    }

    @Override
    
    public VirtualFile[] getVirtualFiles() {
        if (!myInitialized) initialize();

        if (myFile == null) {
            return VirtualFile.EMPTY_ARRAY;
        }

        myFileArray[0] = myFile;
        return myFileArray;
    }

    @Override
    
    public String[] getUrls() {
        return new String[]{myUrl};
    }

    @Override
    public boolean isValid() {
        if (!myInitialized) {
            initialize();
        }

        return myFile != null && myFile.isValid();
    }

    @Override
    public void update() {
        initialize();
    }

    private void initialize() {
        myInitialized = true;

        if (myFile == null || !myFile.isValid()) {
            myFile = VirtualFileManager.getInstance().findFileByUrl(myUrl);
            if (myFile != null && !canHaveChildren()) {
                myFile = null;
            }
        }
    }

    private boolean canHaveChildren() {
        return myFile.getFileSystem().getProtocol().equals(URLUtil.HTTP_PROTOCOL) || myFile.isDirectory();
    }

    public String getUrl() {
        return myUrl;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        String url = element.getAttributeValue(ATTRIBUTE_URL);
        myUrl = migrateJdkAnnotationsToCommunityForDevIdea(url);
    }

    // hack to migrate internal IDEA jdk annos dir from IDEA_PROJECT_HOME/jdkAnnotations to IDEA_PROJECT_HOME/community/java/jdkAnnotations
    private static String migrateJdkAnnotationsToCommunityForDevIdea(String url) {
        File root = new File(VfsUtilCore.urlToPath(url) + "/..");
        boolean isOldJdkAnnotations = new File(root, "community/java/jdkAnnotations").exists()
                && new File(root, "idea.iml").exists()
                && new File(root, "testData").exists();
        if (isOldJdkAnnotations) {
            return VfsUtilCore.pathToUrl(PathUtil.getCanonicalPath(VfsUtilCore.urlToPath(url + "/../community/java/jdkAnnotations")));
        }
        return url;
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        if (!myInitialized) {
            initialize();
        }

        element.setAttribute(ATTRIBUTE_URL, myUrl);
    }
}
