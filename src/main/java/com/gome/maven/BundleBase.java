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
package com.gome.maven;

import com.gome.maven.openapi.util.SystemInfoRt;


import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author yole
 */
public abstract class BundleBase {
    public static final char MNEMONIC = 0x1B;

    public static boolean assertKeyIsFound = false;

    public static String messageOrDefault( final ResourceBundle bundle,
                                           String key,
                                           final String defaultValue,
                                           Object... params) {
        if (bundle == null) return defaultValue;

        String value;
        try {
            value = bundle.getString(key);
        }
        catch (MissingResourceException e) {
            if (defaultValue != null) {
                value = defaultValue;
            }
            else {
                value = "!" + key + "!";
                if (assertKeyIsFound) {
                    assert false : "'" + key + "' is not found in " + bundle;
                }
            }
        }

        value = replaceMnemonicAmpersand(value);

        return format(value, params);
    }

    
    public static String format( String value,  Object... params) {
        if (params.length > 0 && value.indexOf('{') >= 0) {
            return MessageFormat.format(value, params);
        }

        return value;
    }

    
    public static String message( ResourceBundle bundle,  String key,  Object... params) {
        return messageOrDefault(bundle, key, null, params);
    }

    public static String replaceMnemonicAmpersand( final String value) {
        if (value == null) {
            //noinspection ConstantConditions
            return null;
        }

        if (value.indexOf('&') >= 0) {
            boolean useMacMnemonic = value.contains("&&");
            StringBuilder realValue = new StringBuilder();
            int i = 0;
            while (i < value.length()) {
                char c = value.charAt(i);
                if (c == '\\') {
                    if (i < value.length() - 1 && value.charAt(i + 1) == '&') {
                        realValue.append('&');
                        i++;
                    }
                    else {
                        realValue.append(c);
                    }
                }
                else if (c == '&') {
                    if (i < value.length() - 1 && value.charAt(i + 1) == '&') {
                        if (SystemInfoRt.isMac) {
                            realValue.append(MNEMONIC);
                        }
                        i++;
                    }
                    else {
                        if (!SystemInfoRt.isMac || !useMacMnemonic) {
                            realValue.append(MNEMONIC);
                        }
                    }
                }
                else {
                    realValue.append(c);
                }
                i++;
            }

            return realValue.toString();
        }
        return value;
    }
}
