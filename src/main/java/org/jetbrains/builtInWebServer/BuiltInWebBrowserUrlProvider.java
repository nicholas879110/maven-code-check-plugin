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

import com.gome.maven.ide.browsers.OpenInBrowserRequest;
import com.gome.maven.ide.browsers.WebBrowserService;
import com.gome.maven.ide.browsers.WebBrowserUrlProvider;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.impl.http.HttpVirtualFile;
import com.gome.maven.psi.FileViewProvider;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.Url;
import com.gome.maven.util.Urls;
import com.gome.maven.util.containers.ContainerUtil;


import org.jetbrains.ide.BuiltInServerManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BuiltInWebBrowserUrlProvider extends WebBrowserUrlProvider implements DumbAware {

  public static List<Url> getUrls( VirtualFile file,  Project project,  String currentAuthority) {
    if (currentAuthority != null && !compareAuthority(currentAuthority)) {
      return Collections.emptyList();
    }

    String path = WebServerPathToFileManager.getInstance(project).getPath(file);
    if (path == null) {
      return Collections.emptyList();
    }

    int effectiveBuiltInServerPort = BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort();
    Url url = Urls.newHttpUrl(currentAuthority == null ? "localhost:" + effectiveBuiltInServerPort : currentAuthority, '/' + project.getName() + '/' + path);
    int defaultPort = BuiltInServerManager.getInstance().getPort();
    if (currentAuthority != null || defaultPort == effectiveBuiltInServerPort) {
      return Collections.singletonList(url);
    }
    return Arrays.asList(url, Urls.newHttpUrl("localhost:" + defaultPort, '/' + project.getName() + '/' + path));
  }

  public static boolean compareAuthority( String currentAuthority) {
    if (StringUtil.isEmpty(currentAuthority)) {
      return false;
    }

    int portIndex = currentAuthority.indexOf(':');
    if (portIndex < 0) {
      return false;
    }

    String host = currentAuthority.substring(0, portIndex);
    if (!BuiltInWebServer.isOwnHostName(host)) {
      return false;
    }

    int port = StringUtil.parseInt(currentAuthority.substring(portIndex + 1), -1);
    return port == BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort() ||
           port == BuiltInServerManager.getInstance().getPort();
  }

  @Override
  public boolean canHandleElement( OpenInBrowserRequest request) {
    if (request.getVirtualFile() instanceof HttpVirtualFile) {
      return true;
    }

    // we must use base language because we serve file - not part of file, but the whole file
    // handlebars, for example, contains HTML and HBS psi trees, so, regardless of context, we should not handle such file
    FileViewProvider viewProvider = request.getFile().getViewProvider();
    return viewProvider.isPhysical() &&
           !(request.getVirtualFile() instanceof LightVirtualFile) &&
           isMyLanguage(viewProvider.getBaseLanguage());
  }

  protected boolean isMyLanguage( Language language) {
    return WebBrowserService.isHtmlOrXmlFile(language);
  }


  @Override
  protected Url getUrl( OpenInBrowserRequest request,  VirtualFile file) throws BrowserException {
    if (file instanceof HttpVirtualFile) {
      return Urls.newFromVirtualFile(file);
    }
    else {
      return ContainerUtil.getFirstItem(getUrls(file, request.getProject(), null));
    }
  }
}
