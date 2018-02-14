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
package org.jetbrains.jps.javac;

import com.gome.maven.openapi.util.io.FileUtilRt;


import org.jetbrains.jps.incremental.BinaryContent;
import org.jetbrains.jps.incremental.Utils;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.net.URI;

/**
 * @author Eugene Zhuravlev
 *         Date: 9/24/11
 */
public final class OutputFileObject extends SimpleJavaFileObject {

  private final JavacFileManager.Context myContext;

  private final File myOutputRoot;
  private final String myRelativePath;
  private final File myFile;

  private final String myClassName;
   private final URI mySourceUri;
  private volatile BinaryContent myContent;
  private final File mySourceFile;
  private final String myEncodingName;

  public OutputFileObject( JavacFileManager.Context context,
                           File outputRoot,
                          String relativePath,
                           File file,
                           Kind kind,
                           String className,
                           final URI sourceUri,
                           final String encodingName) {
    this(context, outputRoot, relativePath, file, kind, className, sourceUri, encodingName, null);
  }

  public OutputFileObject( JavacFileManager.Context context,
                           File outputRoot,
                          String relativePath,
                           File file,
                           Kind kind,
                           String className,
                           final URI srcUri,
                           final String encodingName,
                           BinaryContent content) {
    super(Utils.toURI(file.getPath()), kind);
    myContext = context;
    mySourceUri = srcUri;
    myContent = content;
    myOutputRoot = outputRoot;
    myRelativePath = relativePath;
    myFile = file;
    myClassName = className != null? className.replace('/', '.') : null;
    mySourceFile = srcUri != null? Utils.convertToFile(srcUri) : null;
    myEncodingName = encodingName;
  }


  public File getOutputRoot() {
    return myOutputRoot;
  }

  public String getRelativePath() {
    return myRelativePath;
  }


  public File getFile() {
    return myFile;
  }


  public String getClassName() {
    return myClassName;
  }


  public File getSourceFile() {
    return mySourceFile;
  }


  public URI getSourceUri() {
    return mySourceUri;
  }

  @Override
  public ByteArrayOutputStream openOutputStream() {
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        try {
          super.close();
        }
        finally {
          myContent = new BinaryContent(buf, 0, size());
          if (myContext != null) {
            myContext.consumeOutputFile(OutputFileObject.this);
          }
        }
      }
    };
  }

  @Override
  public InputStream openInputStream() throws IOException {
    final BinaryContent bytes = myContent;
    if (bytes != null) {
      return new ByteArrayInputStream(bytes.getBuffer(), bytes.getOffset(), bytes.getLength());
    }
    return new BufferedInputStream(new FileInputStream(myFile));
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
    final BinaryContent content = myContent;
    if (content != null) {
      return new String(content.getBuffer(), content.getOffset(), content.getLength());
    }
    return FileUtilRt.loadFile(myFile, myEncodingName, false);
  }


  public BinaryContent getContent() {
    return myContent;
  }

  public void updateContent( byte[] updatedContent) {
    myContent = new BinaryContent(updatedContent, 0, updatedContent.length);
  }

  @Override
  public int hashCode() {
    return toUri().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof JavaFileObject && toUri().equals(((JavaFileObject)obj).toUri());
  }
}
