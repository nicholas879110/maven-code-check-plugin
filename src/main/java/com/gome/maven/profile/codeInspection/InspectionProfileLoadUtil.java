/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.profile.codeInspection;

import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.codeInspection.ex.InspectionToolRegistrar;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.profile.Profile;
import com.gome.maven.profile.ProfileManager;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;

public class InspectionProfileLoadUtil {
     private static final String PROFILE_NAME_TAG = "profile_name";
     public static final String PROFILE_TAG = "profile";

    private static String getProfileName( File file,  Element element) {
        String name = getRootElementAttribute(PROFILE_NAME_TAG, element);
        return name != null ? name : FileUtil.getNameWithoutExtension(file);
    }

    private static String getRootElementAttribute( Element element,  String name) {
        return element.getAttributeValue(name);
    }

    
    private static String getRootElementAttribute( String name,  Element element) {
        return getRootElementAttribute(element, name);
    }

    
    public static String getProfileName( Element element) {
        String name = getRootElementAttribute(element, PROFILE_NAME_TAG);
        if (name != null) return name;
        return "unnamed";
    }

    
    public static Profile load( File file,
                                InspectionToolRegistrar registrar,
                                ProfileManager profileManager) throws JDOMException, IOException, InvalidDataException {
        Element element = JDOMUtil.loadDocument(file).getRootElement();
        InspectionProfileImpl profile = new InspectionProfileImpl(getProfileName(file, element), registrar, profileManager);
        final Element profileElement = element.getChild(PROFILE_TAG);
        if (profileElement != null) {
            element = profileElement;
        }
        profile.readExternal(element);
        return profile;
    }
}
