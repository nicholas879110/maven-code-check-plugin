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
package org.jetbrains.builtInWebServer;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;


import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class WebServerFileHandler {
  static final ExtensionPointName<WebServerFileHandler> EP_NAME = ExtensionPointName.create("org.jetbrains.webServerFileHandler");

  //   open val pageFileExtensions: Array<String>
  // get() = emptyArray()
  protected List<String> pageFileExtensions() {
    return Collections.emptyList();
  }

  public abstract boolean process( VirtualFile file,
                                   CharSequence canonicalRequestPath,
                                   Project project,
                                   FullHttpRequest request,
                                   Channel channel,
                                  boolean isCustomHost) throws IOException;
}