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
package com.gome.maven.spellchecker;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.spellchecker.dictionary.Loader;
import com.gome.maven.util.Consumer;

import java.io.*;

public class StreamLoader implements Loader {

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.spellchecker.StreamLoader");

    private final InputStream stream;
    private final String name;

    public StreamLoader(InputStream stream, String name) {
        this.stream = stream;
        this.name=name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void load( Consumer<String> consumer) {
        DataInputStream in = new DataInputStream(stream);
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(in, CharsetToolkit.UTF8_CHARSET));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                consumer.consume(strLine);
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
        finally {
            try {
                br.close();
            }
            catch (IOException ignored) {

            }
        }
    }

}

