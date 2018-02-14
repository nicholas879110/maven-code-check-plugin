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

/**
 * @author cdr
 */
package com.gome.maven.ide.ui;

import com.gome.maven.openapi.ui.Splitter;
import com.gome.maven.openapi.ui.SplitterProportionsData;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.DimensionService;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.text.StringTokenizer;
import com.gome.maven.util.xmlb.Converter;
import com.gome.maven.util.xmlb.annotations.Tag;
import org.jdom.Element;

import java.awt.*;
import java.util.List;

@Tag("splitter-proportions")
public class SplitterProportionsDataImpl implements SplitterProportionsData {
    private static final String DATA_VERSION = "1";
     private static final String ATTRIBUTE_PROPORTIONS = "proportions";
     private static final String ATTRIBUTE_VERSION = "version";

    private List<Float> proportions = new SmartList<Float>();

    @Override
    public void saveSplitterProportions(Component root) {
        proportions.clear();
        doSaveSplitterProportions(root);
    }

    private void doSaveSplitterProportions(Component root) {
        if (root instanceof Splitter) {
            Float prop = ((Splitter)root).getProportion();
            proportions.add(prop);
        }
        if (root instanceof Container) {
            Component[] children = ((Container)root).getComponents();
            for (Component child : children) {
                doSaveSplitterProportions(child);
            }
        }
    }

    @Override
    public void restoreSplitterProportions(Component root) {
        restoreSplitterProportions(root, 0);
    }

    private int restoreSplitterProportions(Component root, int index) {
        if (root instanceof Splitter) {
            if (proportions.size() <= index) return index;
            ((Splitter)root).setProportion(proportions.get(index++).floatValue());
        }
        if (root instanceof Container) {
            Component[] children = ((Container)root).getComponents();
            for (Component child : children) {
                index = restoreSplitterProportions(child, index);
            }
        }
        return index;
    }

    @Override
    public void externalizeToDimensionService(String key) {
        for (int i = 0; i < proportions.size(); i++) {
            float proportion = proportions.get(i).floatValue();
            String serviceKey = key + "."+i;
            int value = (int)(proportion * 1000);
            DimensionService.getInstance().setExtendedState(serviceKey, value);
        }
    }
    @Override
    public void externalizeFromDimensionService(String key) {
        proportions.clear();
        for (int i = 0; ;i++) {
            String serviceKey = key + "."+i;
            int value = DimensionService.getInstance().getExtendedState(serviceKey);
            if (value == -1) break;
            proportions.add(new Float(value * 0.001));
        }
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        proportions.clear();
        String prop = element.getAttributeValue(ATTRIBUTE_PROPORTIONS);
        String version = element.getAttributeValue(ATTRIBUTE_VERSION);
        if (prop != null && Comparing.equal(version, DATA_VERSION)) {
            StringTokenizer tokenizer = new StringTokenizer(prop, ",");
            while (tokenizer.hasMoreTokens()) {
                String p = tokenizer.nextToken();
                proportions.add(Float.valueOf(p));
            }
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        StringBuilder result = new StringBuilder();
        String sep = "";
        for (Float proportion : proportions) {
            result.append(sep);
            result.append(proportion);
            sep = ",";
        }
        element.setAttribute(ATTRIBUTE_PROPORTIONS, result.toString());
        element.setAttribute(ATTRIBUTE_VERSION, DATA_VERSION);
    }

    public static final class SplitterProportionsConverter extends Converter<SplitterProportionsDataImpl> {
        
        @Override
        public SplitterProportionsDataImpl fromString( String value) {
            SplitterProportionsDataImpl data = new SplitterProportionsDataImpl();
            StringTokenizer tokenizer = new StringTokenizer(value, ",");
            while (tokenizer.hasMoreTokens()) {
                data.proportions.add(Float.valueOf(tokenizer.nextToken()));
            }
            return data;
        }

        
        @Override
        public String toString( SplitterProportionsDataImpl data) {
            StringBuilder result = new StringBuilder();
            String sep = "";
            for (Float proportion : data.proportions) {
                result.append(sep);
                result.append(proportion);
                sep = ",";
            }
            return result.toString();
        }
    }

    public List<Float> getProportions() {
        return proportions;
    }

    public void setProportions(final List<Float> proportions) {
        this.proportions = proportions;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SplitterProportionsDataImpl && ((SplitterProportionsDataImpl)obj).getProportions().equals(proportions);
    }
}