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
package com.gome.maven.spellchecker;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.spellchecker.dictionary.Loader;
import com.gome.maven.util.Consumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
public class FileLoader implements Loader {

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.spellchecker.FileLoader");

    private final String url;
    private final String name;

    public FileLoader(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void load( Consumer<String> consumer) {
        File file = new File(url);
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            StreamLoader loader = new StreamLoader(stream, file.getName());
            loader.load(consumer);
        }
        catch (Exception e) {
            LOG.error(e);
        }
        finally {
            try {
                stream.close();
            }
            catch (IOException ignored) {
            }
        }
    }
}
