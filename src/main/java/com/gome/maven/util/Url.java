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
package com.gome.maven.util;


// We don't use Java URI due to problem - http://cns-etuat-2.localnet.englishtown.com/school/e12/#school/45383/201/221/382?c=countrycode=cc|culturecode=en-us|partnercode=mkge
// it is illegal URI (fragment before query), but we must support such URI
// Semicolon as parameters separator is supported (WEB-6671)
public interface Url {
    
    String getPath();

    boolean isInLocalFileSystem();

    String toDecodedForm();

    
    String toExternalForm();

    
    String getScheme();

    
    String getAuthority();

    
    String getParameters();

    boolean equalsIgnoreParameters( Url url);

    boolean equalsIgnoreCase( Url url);

    
    Url trimParameters();

    int hashCodeCaseInsensitive();
}
