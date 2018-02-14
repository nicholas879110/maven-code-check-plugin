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
package org.jetbrains.jps.builders;



import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.incremental.CompileContext;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public interface BuildRootIndex {


  <R extends BuildRootDescriptor> List<R> getTargetRoots( BuildTarget<R> target,  CompileContext context);


  <R extends BuildRootDescriptor> List<R> getTempTargetRoots( BuildTarget<R> target,  CompileContext context);


  <R extends BuildRootDescriptor> List<R> getRootDescriptors( File root,  Collection<? extends BuildTargetType<? extends BuildTarget<R>>> types,
                                                              CompileContext context);

  <R extends BuildRootDescriptor> void associateTempRoot( CompileContext context,  BuildTarget<R> target,  R root);


  Collection<? extends BuildRootDescriptor> clearTempRoots( CompileContext context);


  <R extends BuildRootDescriptor> R findParentDescriptor( File file,  Collection<? extends BuildTargetType<? extends BuildTarget<R>>> types,
                                                          CompileContext context);


  <R extends BuildRootDescriptor> Collection<R> findAllParentDescriptors( File file,
                                                                          Collection<? extends BuildTargetType<? extends BuildTarget<R>>> types,
                                                                          CompileContext context);


  <R extends BuildRootDescriptor> Collection<R> findAllParentDescriptors( File file,  CompileContext context);


  JavaSourceRootDescriptor findJavaRootDescriptor( CompileContext context, File file);


  FileFilter getRootFilter( BuildRootDescriptor descriptor);

  boolean isDirectoryAccepted( File dir,  BuildRootDescriptor descriptor);

  boolean isFileAccepted( File file,  BuildRootDescriptor descriptor);
}
