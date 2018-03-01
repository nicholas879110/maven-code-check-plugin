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
package com.gome.maven.openapi.projectRoots;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.IconUtil;
import org.jdom.Element;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class SdkType implements SdkTypeId {
    public static ExtensionPointName<SdkType> EP_NAME = ExtensionPointName.create("com.gome.maven.sdkType");

    private final String myName;

    /**
     * @return path to set up file chooser to or null if not applicable
     */
    
    public abstract String suggestHomePath();

    public Collection<String> suggestHomePaths() {
        String s = suggestHomePath();
        return s == null ? Collections.<String>emptyList() : Collections.singletonList(s);
    }

    /**
     * If a path selected in the file chooser is not a valid SDK home path, returns an adjusted version of the path that is again
     * checked for validity.
     *
     * @param homePath the path selected in the file chooser.
     * @return the path to be used as the SDK home.
     */

    public String adjustSelectedSdkHome(String homePath) {
        return homePath;
    }

    public abstract boolean isValidSdkHome(String path);


    @Override
    
    public String getVersionString( Sdk sdk) {
        return getVersionString(sdk.getHomePath());
    }

    
    public String getVersionString(String sdkHome){
        return null;
    }

    public abstract String suggestSdkName(String currentSdkName, String sdkHome);

    public void setupSdkPaths( Sdk sdk) {}

    public boolean setupSdkPaths(final Sdk sdk, final SdkModel sdkModel) {
        setupSdkPaths(sdk);
        return true;
    }

    /**
     * @return Configurable object for the sdk's additional data or null if not applicable
     */
    
    public abstract AdditionalDataConfigurable createAdditionalDataConfigurable(SdkModel sdkModel, SdkModificator sdkModificator);

    
    public SdkAdditionalData loadAdditionalData(Element additional) {
        return null;
    }

    @Override
    
    public SdkAdditionalData loadAdditionalData( Sdk currentSdk, Element additional) {
        return loadAdditionalData(additional);
    }

    public SdkType(  String name) {
        myName = name;
    }

    
    @Override
    public String getName() {
        return myName;
    }

    public abstract String getPresentableName();

    public Icon getIcon() {
        return null;
    }

    

    public String getHelpTopic() {
        return "preferences.jdks";
    }

    public Icon getIconForAddAction() {
        return IconUtil.getAddIcon();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SdkType)) return false;

        final SdkType sdkType = (SdkType)o;

        if (!myName.equals(sdkType.myName)) return false;

        return true;
    }

    public int hashCode() {
        return myName.hashCode();
    }

    public String toString() {
        return getName();
    }

    public FileChooserDescriptor getHomeChooserDescriptor() {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
            @Override
            public void validateSelectedFiles(VirtualFile[] files) throws Exception {
                if (files.length != 0){
                    final String selectedPath = files[0].getPath();
                    boolean valid = isValidSdkHome(selectedPath);
                    if (!valid){
                        valid = isValidSdkHome(adjustSelectedSdkHome(selectedPath));
                        if (!valid) {
                            String message = files[0].isDirectory()
                                    ? ProjectBundle.message("sdk.configure.home.invalid.error", getPresentableName())
                                    : ProjectBundle.message("sdk.configure.home.file.invalid.error", getPresentableName());
                            throw new Exception(message);
                        }
                    }
                }
            }
        };
        descriptor.setTitle(ProjectBundle.message("sdk.configure.home.title", getPresentableName()));
        return descriptor;
    }


    public String getHomeFieldLabel() {
        return ProjectBundle.message("sdk.configure.type.home.path", getPresentableName());
    }

    
    public String getDefaultDocumentationUrl( final Sdk sdk) {
        return null;
    }

    public static SdkType[] getAllTypes() {
        List<SdkType> allTypes = new ArrayList<SdkType>();
        Collections.addAll(allTypes, ApplicationManager.getApplication().getComponents(SdkType.class));
        Collections.addAll(allTypes, Extensions.getExtensions(EP_NAME));
        return allTypes.toArray(new SdkType[allTypes.size()]);
    }

    public static <T extends SdkType> T findInstance(final Class<T> sdkTypeClass) {
        for (SdkType sdkType : Extensions.getExtensions(EP_NAME)) {
            if (sdkTypeClass.equals(sdkType.getClass())) {
                //noinspection unchecked
                return (T)sdkType;
            }
        }
        assert false;
        return null;
    }

    public boolean isRootTypeApplicable(final OrderRootType type) {
        return true;
    }

    /**
     * If this method returns true, instead of showing the standard file path chooser when a new SDK of the type is created,
     * the {@link #showCustomCreateUI} method is called.
     *
     * @return true if the custom create UI is supported, false otherwise.
     * @since 12.0
     */
    public boolean supportsCustomCreateUI() {
        return false;
    }

    /**
     * Shows the custom SDK create UI. The returned SDK needs to have the correct name and home path; the framework will call
     * setupSdkPaths() on the returned SDK.
     *
     * @param sdkModel the list of SDKs currently displayed in the configuration dialog.
     * @param parentComponent the parent component for showing the dialog.
     * @param sdkCreatedCallback the callback to which the created SDK is passed.
     * @since 12.0
     */
    public void showCustomCreateUI(SdkModel sdkModel, JComponent parentComponent, Consumer<Sdk> sdkCreatedCallback) {
    }

    /**
     * Checks if the home directory of the specified SDK is valid. By default, checks that the directory points to a valid local
     * path. Can be overridden for remote SDKs.
     *
     * @param sdk the SDK to validate the path for.
     * @return true if the home path is valid, false otherwise.
     * @since 12.1
     */
    public boolean sdkHasValidPath( Sdk sdk) {
        VirtualFile homeDir = sdk.getHomeDirectory();
        return homeDir != null && homeDir.isValid();
    }

    public String sdkPath(VirtualFile homePath) {
        return homePath.getPath();
    }
}
