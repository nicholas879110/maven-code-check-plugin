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

import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.util.SystemInfoRt;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.io.FileUtilRt;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.util.text.StringUtilRt;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.PathUtilRt;
import com.gome.maven.util.UriUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.io.URLUtil;
import com.gome.maven.util.net.NetUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtf8Writer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedStream;


import org.jetbrains.builtInWebServer.ssi.SsiExternalResolver;
import org.jetbrains.builtInWebServer.ssi.SsiProcessor;
import org.jetbrains.ide.BuiltInServerManagerImpl;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.FileResponses;
import org.jetbrains.io.NettyUtil;
import org.jetbrains.io.Responses;
import org.jetbrains.notification.SingletonNotificationManager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static org.jetbrains.io.Responses.addKeepAliveIfNeed;

public final class BuiltInWebServer extends HttpRequestHandler {
  static final Logger LOG = Logger.getInstance(BuiltInWebServer.class);


  public static VirtualFile findIndexFile( VirtualFile basedir) {
    VirtualFile[] children = basedir.getChildren();
    if (children == null || children.length == 0) {
      return null;
    }

    for (String indexNamePrefix : new String[]{"index.", "default."}) {
      VirtualFile index = null;
      String preferredName = indexNamePrefix + "html";
      for (VirtualFile child : children) {
        if (!child.isDirectory()) {
          String name = child.getName();
          //noinspection IfStatementWithIdenticalBranches
          if (name.equals(preferredName)) {
            return child;
          }
          else if (index == null && name.startsWith(indexNamePrefix)) {
            index = child;
          }
        }
      }
      if (index != null) {
        return index;
      }
    }
    return null;
  }

  public boolean isAccessible( HttpRequest request) { return NettyUtil.isLocalOrigin(request, false, true); }

  @Override
  public boolean isSupported( FullHttpRequest request) {
    return super.isSupported(request) || request.method() == HttpMethod.POST;
  }

  @Override
  public boolean process( QueryStringDecoder urlDecoder,  FullHttpRequest request,  ChannelHandlerContext context) {
    String host = request.headers().get(HttpHeaderNames.HOST);
    if (StringUtil.isEmpty(host)) {
      return false;
    }

    int portIndex = host.indexOf(':');
    if (portIndex > 0) {
      host = host.substring(0, portIndex);
    }

    String projectName;
    boolean isIpv6 = host.charAt(0) == '[' && host.length() > 2 && host.charAt(host.length() - 1) == ']';
    if (isIpv6) {
      host = host.substring(1, host.length() - 1);
    }

    if (isIpv6 || Character.digit(host.charAt(0), 10) != -1 || host.charAt(0) == ':' || isOwnHostName(host)) {
      if (urlDecoder.path().length() < 2) {
        return false;
      }
      projectName = null;
    }
    else {
      projectName = host;
    }
    return doProcess(request, context, projectName);
  }

  public static boolean isOwnHostName( String host) {
    if (NetUtils.isLocalhost(host)) {
      return true;
    }

    try {
      InetAddress address = InetAddress.getByName(host);
      if (host.equals(address.getHostAddress()) || host.equalsIgnoreCase(address.getCanonicalHostName())) {
        return true;
      }

      String localHostName = InetAddress.getLocalHost().getHostName();
      // WEB-8889
      // develar.local is own host name: develar. equals to "develar.labs.gome.maven.net" (canonical host name)
      return localHostName.equalsIgnoreCase(host) ||
             (host.endsWith(".local") && localHostName.regionMatches(true, 0, host, 0, host.length() - ".local".length()));
    }
    catch (IOException ignored) {
      return false;
    }
  }

  // private val notificationManager by lazy {
  // SingletonNotificationManager(BuiltInServerManagerImpl.NOTIFICATION_GROUP.getValue(), NotificationType.INFORMATION, null)
  // }
  private static final SingletonNotificationManager
    notificationManager = new SingletonNotificationManager(BuiltInServerManagerImpl.NOTIFICATION_GROUP.getValue(), NotificationType.INFORMATION, null);
  // internal fun isActivatable() = Registry.`is`("ide.built.in.web.server.activatable", false)
  static boolean isActivatable() { return Registry.is("ide.built.in.web.server.activatable", true); }

  private static boolean doProcess( FullHttpRequest request,  ChannelHandlerContext context,  String projectName) {
    final String decodedPath = URLUtil.unescapePercentSequences(UriUtil.trimParameters(request.uri()));
    int offset;
    boolean emptyPath;
    boolean isCustomHost = projectName != null;
    if (isCustomHost) {
      // host mapped to us
      offset = 0;
      emptyPath = decodedPath.isEmpty();
    }
    else {
      offset = decodedPath.indexOf('/', 1);
      projectName = decodedPath.substring(1, offset == -1 ? decodedPath.length() : offset);
      emptyPath = offset == -1;
    }

    Project project = findProject(projectName, isCustomHost);
    if (project == null) {
      return false;
    }

    if (isActivatable() && !PropertiesComponent.getInstance().getBoolean("ide.built.in.web.server.active", false)) {
      notificationManager.notify("Built-in web server is deactivated, to activate, please use Open in Browser", (Project)null);
      return false;
    }

    if (emptyPath) {
      if (!SystemInfoRt.isFileSystemCaseSensitive) {
        // may be passed path is not correct
        projectName = project.getName();
      }

      // we must redirect "jsdebug" to "jsdebug/" as nginx does, otherwise browser will treat it as file instead of directory, so, relative path will not work
      WebServerPathHandler.redirectToDirectory(request, context.channel(), projectName);
      return true;
    }

    final String path = toIdeaPath(decodedPath, offset);
    if (path == null) {
      LOG.warn(decodedPath + " is not valid");
      Responses.sendStatus(HttpResponseStatus.NOT_FOUND, context.channel(), request);
      return true;
    }

    for (WebServerPathHandler pathHandler : WebServerPathHandler.EP_NAME.getExtensions()) {
      try {
        if (pathHandler.process(path, project, request, context, projectName, decodedPath, isCustomHost)) {
          return true;
        }
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
    return false;
  }

  static boolean canBeAccessedDirectly(String path) {
    for (WebServerFileHandler fileHandler : WebServerFileHandler.EP_NAME.getExtensions()) {
      for (String ext: fileHandler.pageFileExtensions()) {
        if (FileUtilRt.extensionEquals(path, ext)) {
          return true;
        }
      }
    }
    return false;
  }

  private static String toIdeaPath(String decodedPath, int offset) {
    // must be absolute path (relative to DOCUMENT_ROOT, i.e. scheme://authority/) to properly canonicalize
    String path = decodedPath.substring(offset);
    if (!path.startsWith("/")) {
      return null;
    }
    return FileUtil.toCanonicalPath(path, '/').substring(1);
  }


  static final class StaticFileHandler extends WebServerFileHandler {
    private SsiProcessor ssiProcessor;

    // override val pageFileExtensions = arrayOf("html", "htm", "shtml")
    private static List<String> ourPageFileExtensions = ContainerUtil.list("html", "htm", "shtml", "stm", "shtm");

    @Override
    protected List<String> pageFileExtensions() {
      return ourPageFileExtensions;
    }

    @Override
    public boolean process( VirtualFile file,
                            CharSequence canonicalRequestPath,
                            Project project,
                            FullHttpRequest request,
                            Channel channel,
                           boolean isCustomHost) throws IOException {
      if (file.isInLocalFileSystem()) {
        CharSequence nameSequence = file.getNameSequence();
        //noinspection SpellCheckingInspection
        if (StringUtilRt.endsWithIgnoreCase(nameSequence, ".shtml") || StringUtilRt.endsWithIgnoreCase(nameSequence, ".stm") || StringUtilRt.endsWithIgnoreCase(nameSequence, ".shtm")) {
          processSsi(file, canonicalRequestPath, project, request, channel, isCustomHost);
          return true;
        }

        FileResponses.sendFile(request, channel, VfsUtilCore.virtualToIoFile(file));
      }
      else {
        HttpResponse response = FileResponses.prepareSend(request, channel, file.getTimeStamp(), file.getPath());
        if (response == null) {
          return true;
        }

        boolean keepAlive = addKeepAliveIfNeed(response, request);
        if (request.method() != HttpMethod.HEAD) {
          HttpHeaderUtil.setContentLength(response, file.getLength());
        }

        channel.write(response);

        if (request.method() != HttpMethod.HEAD) {
          channel.write(new ChunkedStream(file.getInputStream()));
        }

        ChannelFuture future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
          future.addListener(ChannelFutureListener.CLOSE);
        }
      }
      return true;
    }

    private void processSsi( VirtualFile file,
                             CharSequence canonicalRequestPath,
                             Project project,
                             FullHttpRequest request,  Channel channel, boolean isCustomHost) throws IOException {
      String path = PathUtilRt.getParentPath(canonicalRequestPath.toString());
      if (!isCustomHost) {
        // remove project name - SSI resolves files only inside current project
        path = path.substring(path.indexOf('/', 1) + 1);
      }

      if (ssiProcessor == null) {
        ssiProcessor = new SsiProcessor(false);
      }

      ByteBuf buffer = channel.alloc().ioBuffer();
      boolean keepAlive;
      boolean releaseBuffer = true;
      try {
        long lastModified = ssiProcessor.process(new SsiExternalResolver(project, request, path, file.getParent()),
                                                 VfsUtilCore.loadText(file), file.getTimeStamp(), new ByteBufUtf8Writer(buffer));

        HttpResponse response = FileResponses.prepareSend(request, channel, lastModified, file.getPath());
        if (response == null) {
          return;
        }

        keepAlive = addKeepAliveIfNeed(response, request);
        if (request.method() != HttpMethod.HEAD) {
          HttpHeaderUtil.setContentLength(response, buffer.readableBytes());
        }

        channel.write(response);

        if (request.method() != HttpMethod.HEAD) {
          releaseBuffer = false;
          channel.write(buffer);
        }
      }
      finally {
        if (releaseBuffer) {
          buffer.release();
        }
      }

      ChannelFuture future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      if (!keepAlive) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    }

    static boolean checkAccess(Channel channel, File file, HttpRequest request, File root) {
      File parent = file;
      do {
        if (!hasAccess(parent)) {
          Responses.sendStatus(HttpResponseStatus.NOT_FOUND, channel, request);
          return false;
        }
        parent = parent.getParentFile();
        if (parent == null) break;
      }
      while (!FileUtil.filesEqual(parent, root));
      return true;
    }

    private static boolean hasAccess(File result) {
      // deny access to any dot prefixed file
      return result.canRead() && !(result.isHidden() || result.getName().startsWith("."));
    }
  }


  private static Project findProject(String projectName, boolean isCustomHost) {
    // user can rename project directory, so, we should support this case - find project by base directory name
    Project candidateByDirectoryName = null;
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      String name = project.getName();
      // domain name is case-insensitive
      if (!project.isDisposed() && ((isCustomHost || !SystemInfoRt.isFileSystemCaseSensitive) ? projectName.equalsIgnoreCase(name) : projectName.equals(name))) {
        return project;
      }

      if (candidateByDirectoryName == null && compareNameAndProjectBasePath(projectName, project)) {
        candidateByDirectoryName = project;
      }
    }
    return candidateByDirectoryName;
  }

  public static boolean compareNameAndProjectBasePath(String projectName, Project project) {
    String basePath = project.getBasePath();
    return basePath != null && basePath.length() > projectName.length() && basePath.endsWith(projectName) && basePath.charAt(basePath.length() - projectName.length() - 1) == '/';
  }
}