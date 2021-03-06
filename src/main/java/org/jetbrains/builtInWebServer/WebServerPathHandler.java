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
package org.jetbrains.builtInWebServer;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VfsUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;


import org.jetbrains.io.Responses;

import java.net.URI;

/**
 * By default {@link WebServerPathToFileManager} will be used to map request to file.
 * If file physically exists in the file system, you must use {@link WebServerRootsProvider}.
 *
 * Consider to extend {@link WebServerPathHandlerAdapter} instead of implement low-level {@link #process)}
 */
public abstract class WebServerPathHandler {
  static final ExtensionPointName<WebServerPathHandler> EP_NAME = ExtensionPointName.create("org.jetbrains.webServerPathHandler");

  public abstract boolean process( String path,
                                   Project project,
                                   FullHttpRequest request,
                                   ChannelHandlerContext context,
                                   String projectName,
                                   String decodedRawPath,
                                  boolean isCustomHost);

  protected static void redirectToDirectory( HttpRequest request,  Channel channel,  String path) {
    FullHttpResponse response = Responses.response(HttpResponseStatus.MOVED_PERMANENTLY);
    URI url = VfsUtil.toUri("http://" + request.headers().get(HttpHeaderNames.HOST) + '/' + path + '/');
    BuiltInWebServer.LOG.assertTrue(url != null);
    response.headers().add(HttpHeaderNames.LOCATION, url.toASCIIString());
    Responses.send(response, channel, request);
  }

  protected static boolean endsWithSlash( String path) {
    return path.charAt(path.length() - 1) == '/';
  }
}