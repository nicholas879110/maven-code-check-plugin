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
package com.siyeh;

import com.gome.maven.CommonBundle;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * @author max
 */
public class InspectionGadgetsBundle {

    public static String message( String key,  Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static Reference<ResourceBundle> ourBundle;
     private static final String BUNDLE = "com.siyeh.InspectionGadgetsBundle";

    private InspectionGadgetsBundle() {
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.gome.maven.reference.SoftReference.dereference(ourBundle);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            ourBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }
}