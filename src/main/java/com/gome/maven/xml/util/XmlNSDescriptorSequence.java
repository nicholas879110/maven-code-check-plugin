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
package com.gome.maven.xml.util;

import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.xml.XmlDocument;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.xml.XmlElementDescriptor;
import com.gome.maven.xml.XmlNSDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 08.09.2003
 * Time: 17:27:43
 * To change this template use Options | File Templates.
 */
public class XmlNSDescriptorSequence implements XmlNSDescriptor{
    final List<XmlNSDescriptor> sequence = new ArrayList<XmlNSDescriptor>();

    public XmlNSDescriptorSequence(){
    }

    public XmlNSDescriptorSequence(XmlNSDescriptor[] descriptors){
        for (final XmlNSDescriptor descriptor : descriptors) {
            add(descriptor);
        }
    }

    public void add(XmlNSDescriptor descriptor){
        sequence.add(descriptor);
    }

    @Override
    public XmlElementDescriptor getElementDescriptor( XmlTag tag){
        for (XmlNSDescriptor descriptor : sequence) {
            final XmlElementDescriptor elementDescriptor = descriptor.getElementDescriptor(tag);
            if (elementDescriptor != null) return elementDescriptor;
        }
        return null;
    }

    @Override
    
    public XmlElementDescriptor[] getRootElementsDescriptors( final XmlDocument document) {
        final List<XmlElementDescriptor> descriptors = new ArrayList<XmlElementDescriptor>();
        for (XmlNSDescriptor descriptor : sequence) {
            ContainerUtil.addAll(descriptors, descriptor.getRootElementsDescriptors(document));
        }

        return descriptors.toArray(new XmlElementDescriptor[descriptors.size()]);
    }

    @Override
    public XmlFile getDescriptorFile(){
        for (XmlNSDescriptor descriptor : sequence) {
            final XmlFile file = descriptor.getDescriptorFile();
            if (file != null) return file;
        }
        return null;
    }

    public List<XmlNSDescriptor> getSequence(){
        return sequence;
    }

    @Override
    public boolean isHierarhyEnabled() {
        for (XmlNSDescriptor descriptor : sequence) {
            if (descriptor.isHierarhyEnabled()) return true;
        }
        return false;
    }

    @Override
    public PsiElement getDeclaration(){
        for (XmlNSDescriptor descriptor : sequence) {
            final PsiElement declaration = descriptor.getDeclaration();
            if (declaration != null) return declaration;
        }
        return null;
    }

    @Override
    public String getName(PsiElement context){
        for (XmlNSDescriptor descriptor : sequence) {
            final String name = descriptor.getName(context);
            if (name != null) return name;
        }
        return null;
    }

    @Override
    public String getName(){
        for (XmlNSDescriptor descriptor : sequence) {
            final String name = descriptor.getName();
            if (name != null) return name;
        }
        return null;
    }

    @Override
    public void init(PsiElement element){
        for (XmlNSDescriptor descriptor : sequence) {
            descriptor.init(element);
        }
    }

    @Override
    public Object[] getDependences(){
        final List<Object> ret = new ArrayList<Object>();
        for (XmlNSDescriptor descriptor : sequence) {
            ContainerUtil.addAll(ret, descriptor.getDependences());
        }
        return ret.toArray();
    }
}