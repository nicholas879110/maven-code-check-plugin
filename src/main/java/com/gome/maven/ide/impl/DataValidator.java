/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.ide.impl;

import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.KeyedLazyInstanceEP;
import com.gome.maven.util.containers.HashMap;

import java.lang.reflect.Array;
import java.util.Map;

public abstract class DataValidator<T> {
    private static boolean ourExtensionsLoaded;

    public static final ExtensionPointName<KeyedLazyInstanceEP<DataValidator>> EP_NAME =
            ExtensionPointName.create("com.gome.maven.dataValidator");

    Logger LOG = Logger.getInstance("#com.gome.maven.ide.impl.DataValidator");

    private static final Map<String, DataValidator> ourValidators = new HashMap<String, DataValidator>();
    private static final DataValidator<VirtualFile> VIRTUAL_FILE_VALIDATOR = new DataValidator<VirtualFile>() {
        public VirtualFile findInvalid(final String dataId, VirtualFile file, final Object dataSource) {
            return file.isValid() ? null : file;
        }
    };
    private static final DataValidator<Project> PROJECT_VALIDATOR = new DataValidator<Project>() {
        @Override
        public Project findInvalid(final String dataId, final Project project, final Object dataSource) {
            return project.isDisposed() ? project : null;
        }
    };

    public abstract T findInvalid(final String dataId, T data, final Object dataSource);

    private static <T> DataValidator<T> getValidator(String dataId) {
        if (!ourExtensionsLoaded) {
            ourExtensionsLoaded = true;
            for (KeyedLazyInstanceEP<DataValidator> ep : Extensions.getExtensions(EP_NAME)) {
                ourValidators.put(ep.key, ep.getInstance());
            }
        }
        return ourValidators.get(dataId);
    }

    public static <T> T findInvalidData(String dataId, Object data, final Object dataSource) {
        if (data == null) return null;
        DataValidator<T> validator = getValidator(dataId);
        if (validator != null) return validator.findInvalid(dataId, (T)data, dataSource);
        return null;
    }

    static {
        ourValidators.put(CommonDataKeys.VIRTUAL_FILE.getName(), VIRTUAL_FILE_VALIDATOR);
        ourValidators.put(CommonDataKeys.VIRTUAL_FILE_ARRAY.getName(), new ArrayValidator<VirtualFile>(VIRTUAL_FILE_VALIDATOR));
        ourValidators.put(CommonDataKeys.PROJECT.getName(), PROJECT_VALIDATOR);
    }

    public static class ArrayValidator<T> extends DataValidator<T[]> {
        private final DataValidator<T> myElementValidator;

        public ArrayValidator(DataValidator<T> elementValidator) {
            myElementValidator = elementValidator;
        }

        public T[] findInvalid(final String dataId, T[] array, final Object dataSource) {
            for (T element : array) {
                if (element == null) {
                    LOG.error(
                            "Data isn't valid. " + dataId + "=null Provided by: " + dataSource.getClass().getName() + " (" + dataSource.toString() + ")");
                }
                T invalid = myElementValidator.findInvalid(dataId, element, dataSource);
                if (invalid != null) {
                    T[] result = (T[])Array.newInstance(array[0].getClass(), 1);
                    result[0] = invalid;
                    return result;
                }
            }
            return null;
        }
    }

}
