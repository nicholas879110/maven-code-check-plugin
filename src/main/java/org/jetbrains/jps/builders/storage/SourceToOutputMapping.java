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
package org.jetbrains.jps.builders.storage;




import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author nik
 */
public interface SourceToOutputMapping {
  void setOutputs( String srcPath,  Collection<String> outputs) throws IOException;

  void setOutput( String srcPath,  String outputPath) throws IOException;

  void appendOutput( String srcPath,  String outputPath) throws IOException;


  void remove( String srcPath) throws IOException;

  void removeOutput( String sourcePath,  String outputPath) throws IOException;



  Collection<String> getSources() throws IOException;


  Collection<String> getOutputs( String srcPath) throws IOException;


  Iterator<String> getSourcesIterator() throws IOException;
}
