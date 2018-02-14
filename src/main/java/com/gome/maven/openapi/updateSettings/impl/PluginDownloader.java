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
package com.gome.maven.openapi.updateSettings.impl;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.plugins.*;
import com.gome.maven.ide.startup.StartupActionScriptManager;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationInfo;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.application.ex.ApplicationInfoEx;
import com.gome.maven.openapi.application.impl.ApplicationInfoImpl;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.PluginId;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.BuildNumber;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.PathUtil;
import com.gome.maven.util.io.HttpRequests;
import com.gome.maven.util.io.ZipUtil;
import org.apache.http.client.utils.URIBuilder;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.UUID;

/**
 * @author anna
 * @since 10-Aug-2007
 */
public class PluginDownloader {
    private static final Logger LOG = Logger.getInstance("#" + PluginDownloader.class.getName());

    private static final String FILENAME = "filename=";

    private final String myPluginId;
    private final String myPluginUrl;
    private final String myPluginName;
    private String myPluginVersion;
    private BuildNumber myBuildNumber;

    private File myFile;
    private File myOldFile;
    private String myDescription;
    private List<PluginId> myDepends;
    private IdeaPluginDescriptor myDescriptor;
    private boolean myForceHttps;

    private PluginDownloader( String pluginId,
                              String pluginUrl,
                              String pluginName,
                              String pluginVersion,
                              BuildNumber buildNumber) {
        myPluginId = pluginId;
        myPluginUrl = pluginUrl;
        myPluginVersion = pluginVersion;
        myPluginName = pluginName;
        myBuildNumber = buildNumber;
    }

    public String getPluginId() {
        return myPluginId;
    }

    public String getPluginVersion() {
        return myPluginVersion;
    }

    public String getPluginName() {
        return myPluginName != null ? myPluginName : myPluginId;
    }

    public BuildNumber getBuildNumber() {
        return myBuildNumber;
    }

    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        myDescription = description;
    }

    public List<PluginId> getDepends() {
        return myDepends;
    }

    public void setDepends(List<PluginId> depends) {
        myDepends = depends;
    }

    public IdeaPluginDescriptor getDescriptor() {
        return myDescriptor;
    }

    public void setDescriptor(IdeaPluginDescriptor descriptor) {
        myDescriptor = descriptor;
    }

    public void setForceHttps(boolean forceHttps) {
        myForceHttps = forceHttps;
    }

    public boolean prepareToInstall( ProgressIndicator indicator) throws IOException {
        if (myFile != null) {
            return true;
        }

        IdeaPluginDescriptor descriptor = null;
        if (!Boolean.getBoolean(StartupActionScriptManager.STARTUP_WIZARD_MODE) && PluginManager.isPluginInstalled(PluginId.getId(myPluginId))) {
            //store old plugins file
            descriptor = PluginManager.getPlugin(PluginId.getId(myPluginId));
            LOG.assertTrue(descriptor != null);
            if (myPluginVersion != null && compareVersionsSkipBroken(descriptor, myPluginVersion) <= 0) {
                LOG.info("Plugin " + myPluginId + ": current version (max) " + myPluginVersion);
                return false;
            }
            myOldFile = descriptor.getPath();
        }

        // download plugin
        String errorMessage = IdeBundle.message("unknown.error");
        try {
            myFile = downloadPlugin(indicator);
        }
        catch (IOException ex) {
            myFile = null;
            LOG.warn(ex);
            errorMessage = ex.getMessage();
        }
        if (myFile == null) {
            if (ApplicationManager.getApplication() != null) {
                final String text = IdeBundle.message("error.plugin.was.not.installed", getPluginName(), errorMessage);
                final String title = IdeBundle.message("title.failed.to.download");
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showErrorDialog(text, title);
                    }
                });
            }
            return false;
        }

        IdeaPluginDescriptorImpl actualDescriptor = loadDescriptionFromJar(myFile);
        if (actualDescriptor != null) {
            InstalledPluginsState state = InstalledPluginsState.getInstanceIfLoaded();
            if (state != null && state.wasUpdated(actualDescriptor.getPluginId())) {
                return false; //already updated
            }

            myPluginVersion = actualDescriptor.getVersion();
            if (descriptor != null && compareVersionsSkipBroken(descriptor, myPluginVersion) <= 0) {
                LOG.info("Plugin " + myPluginId + ": current version (max) " + myPluginVersion);
                return false; //was not updated
            }

            setDescriptor(actualDescriptor);

            if (PluginManagerCore.isIncompatible(actualDescriptor, myBuildNumber)) {
                LOG.info("Plugin " + myPluginId + " is incompatible with current installation " +
                        "(since:" + actualDescriptor.getSinceBuild() + " until:" + actualDescriptor.getUntilBuild() + ")");
                return false; //host outdated plugins, no compatible plugin for new version
            }
        }
        return true;
    }

    public static int compareVersionsSkipBroken(IdeaPluginDescriptor descriptor, String newPluginVersion) {
        int state = StringUtil.compareVersionNumbers(newPluginVersion, descriptor.getVersion());
        if (PluginManagerCore.isBrokenPlugin(descriptor) && state < 0) {
            state = 1;
        }
        return state;
    }

    
    public static IdeaPluginDescriptorImpl loadDescriptionFromJar(final File file) throws IOException {
        IdeaPluginDescriptorImpl descriptor = PluginManagerCore.loadDescriptorFromJar(file);
        if (descriptor == null) {
            if (file.getName().endsWith(".zip")) {
                final File outputDir = FileUtil.createTempDirectory("plugin", "");
                try {
                    ZipUtil.extract(file, outputDir, null);
                    final File[] files = outputDir.listFiles();
                    if (files != null && files.length == 1) {
                        descriptor = PluginManagerCore.loadDescriptor(files[0], PluginManagerCore.PLUGIN_XML);
                    }
                }
                finally {
                    FileUtil.delete(outputDir);
                }
            }
        }
        return descriptor;
    }

    public void install() throws IOException {
        LOG.assertTrue(myFile != null);
        if (myOldFile != null) {
            // add command to delete the 'action script' file
            StartupActionScriptManager.ActionCommand deleteOld = new StartupActionScriptManager.DeleteCommand(myOldFile);
            StartupActionScriptManager.addActionCommand(deleteOld);
        }
        PluginInstaller.install(myFile, getPluginName(), true);
        InstalledPluginsState state = InstalledPluginsState.getInstanceIfLoaded();
        if (state != null) {
            state.onPluginInstall(myDescriptor);
        }
    }

    
    private File downloadPlugin( final ProgressIndicator indicator) throws IOException {
        File pluginsTemp = new File(PathManager.getPluginTempPath());
        if (!pluginsTemp.exists() && !pluginsTemp.mkdirs()) {
            throw new IOException(IdeBundle.message("error.cannot.create.temp.dir", pluginsTemp));
        }
        final File file = FileUtil.createTempFile(pluginsTemp, "plugin_", "_download", true, false);

        indicator.checkCanceled();
        indicator.setText2(IdeBundle.message("progress.downloading.plugin", getPluginName()));

        return HttpRequests.request(myPluginUrl).gzip(false).forceHttps(myForceHttps).connect(new HttpRequests.RequestProcessor<File>() {
            @Override
            public File process( HttpRequests.Request request) throws IOException {
                request.saveToFile(file, indicator);

                String fileName = guessFileName(request.getConnection(), file);
                File newFile = new File(file.getParentFile(), fileName);
                FileUtil.rename(file, newFile);
                return newFile;
            }
        });
    }

    
    private String guessFileName( URLConnection connection,  File file) throws IOException {
        String fileName = null;

        final String contentDisposition = connection.getHeaderField("Content-Disposition");
        LOG.debug("header: " + contentDisposition);

        if (contentDisposition != null && contentDisposition.contains(FILENAME)) {
            final int startIdx = contentDisposition.indexOf(FILENAME);
            final int endIdx = contentDisposition.indexOf(';', startIdx);
            fileName = contentDisposition.substring(startIdx + FILENAME.length(), endIdx > 0 ? endIdx : contentDisposition.length());

            if (StringUtil.startsWithChar(fileName, '\"') && StringUtil.endsWithChar(fileName, '\"')) {
                fileName = fileName.substring(1, fileName.length() - 1);
            }
        }

        if (fileName == null) {
            // try to find a filename in an URL
            final String usedURL = connection.getURL().toString();
            fileName = usedURL.substring(usedURL.lastIndexOf('/') + 1);
            if (fileName.length() == 0 || fileName.contains("?")) {
                fileName = myPluginUrl.substring(myPluginUrl.lastIndexOf('/') + 1);
            }
        }

        if (!PathUtil.isValidFileName(fileName)) {
            FileUtil.delete(file);
            throw new IOException("Invalid filename returned by a server");
        }

        return fileName;
    }

    // creators-converters

    public static PluginDownloader createDownloader( IdeaPluginDescriptor descriptor) throws IOException {
        return createDownloader(descriptor, null, null);
    }

    
    public static PluginDownloader createDownloader( IdeaPluginDescriptor descriptor,
                                                     String host,
                                                     BuildNumber buildNumber) throws IOException {
        try {
            String url = getUrl(descriptor, host, buildNumber);
            String id = descriptor.getPluginId().getIdString();
            PluginDownloader downloader = new PluginDownloader(id, url, descriptor.getName(), descriptor.getVersion(), buildNumber);
            downloader.setDescriptor(descriptor);
            downloader.setDescription(descriptor.getDescription());
            downloader.setDepends(((PluginNode)descriptor).getDepends());
            return downloader;
        }
        catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    
    private static String getUrl( IdeaPluginDescriptor descriptor,
                                  String host,
                                  BuildNumber buildNumber) throws URISyntaxException, MalformedURLException {
        if (host != null && descriptor instanceof PluginNode) {
            String url = ((PluginNode)descriptor).getDownloadUrl();
            return new URI(url).isAbsolute() ? url : new URL(new URL(host), url).toExternalForm();
        }
        else {
            Application app = ApplicationManager.getApplication();
            ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();

            String buildNumberAsString = buildNumber != null ? buildNumber.asString() :
                    app != null ? ApplicationInfo.getInstance().getApiVersion() :
                            appInfo.getBuild().asString();

            String uuid = app != null ? UpdateChecker.getInstallationUID(PropertiesComponent.getInstance()) : UUID.randomUUID().toString();

            URIBuilder uriBuilder = new URIBuilder(appInfo.getPluginsDownloadUrl());
            uriBuilder.addParameter("action", "download");
            uriBuilder.addParameter("id", descriptor.getPluginId().getIdString());
            uriBuilder.addParameter("build", buildNumberAsString);
            uriBuilder.addParameter("uuid", uuid);
            return uriBuilder.build().toString();
        }
    }

    
    public static PluginNode createPluginNode( String host,  PluginDownloader downloader) {
        IdeaPluginDescriptor descriptor = downloader.getDescriptor();
        if (descriptor instanceof PluginNode) {
            return (PluginNode)descriptor;
        }

        PluginNode node = new PluginNode(PluginId.getId(downloader.getPluginId()));
        node.setName(downloader.getPluginName());
        node.setVersion(downloader.getPluginVersion());
        node.setRepositoryName(host);
        node.setDownloadUrl(downloader.myPluginUrl);
        node.setDepends(downloader.getDepends(), null);
        node.setDescription(downloader.getDescription());
        return node;
    }
}
