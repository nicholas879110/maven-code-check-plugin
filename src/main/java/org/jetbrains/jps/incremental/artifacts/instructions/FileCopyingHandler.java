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
package org.jetbrains.jps.incremental.artifacts.instructions;

import com.gome.maven.openapi.util.io.FileUtilRt;

import org.jetbrains.jps.incremental.CompileContext;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author nik
 */
public abstract class FileCopyingHandler {
  public static final FileCopyingHandler DEFAULT = new FilterCopyHandler(FileUtilRt.ALL_FILES);

  public abstract void copyFile( File from,  File to,  CompileContext context) throws IOException;

  public abstract void writeConfiguration( PrintWriter out);


  public FileFilter createFileFilter() {
    return FileUtilRt.ALL_FILES;
  }
}
