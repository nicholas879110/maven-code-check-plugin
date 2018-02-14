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

package com.gome.maven.util.xmlb;


import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.openapi.util.io.StreamUtil;
import com.gome.maven.util.io.URLUtil;
import org.jdom.*;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JDOMXIncluder {
    private static final Logger LOG = Logger.getInstance(JDOMXIncluder.class);
    public static final PathResolver DEFAULT_PATH_RESOLVER = new PathResolver() {
        
        @Override
        public URL resolvePath( String relativePath,  String base) {
            try {
                if (base != null) {
                    return new URL(new URL(base), relativePath);
                }
                else {
                    return new URL(relativePath);
                }
            }
            catch (MalformedURLException ex) {
                throw new XIncludeException(ex);
            }
        }
    };

     private static final String HTTP_WWW_W3_ORG_2001_XINCLUDE = "http://www.w3.org/2001/XInclude";
     private static final String XI = "xi";
     private static final String INCLUDE = "include";
     private static final String HREF = "href";
     private static final String BASE = "base";
     private static final String PARSE = "parse";
     private static final String TEXT = "text";
     private static final String XML = "xml";
     private static final String ENCODING = "encoding";
     private static final String XPOINTER = "xpointer";

    public static final Namespace XINCLUDE_NAMESPACE = Namespace.getNamespace(XI, HTTP_WWW_W3_ORG_2001_XINCLUDE);
    private final boolean myIgnoreMissing;
    private final PathResolver myPathResolver;

    private JDOMXIncluder(boolean ignoreMissing, PathResolver pathResolver) {
        myIgnoreMissing = ignoreMissing;
        myPathResolver = pathResolver;
    }

    public static Document resolve(Document original, String base) throws XIncludeException {
        return resolve(original, base, false);
    }

    public static Document resolve(Document original, String base, boolean ignoreMissing) throws XIncludeException {
        return resolve(original, base, ignoreMissing, DEFAULT_PATH_RESOLVER);
    }

    public static Document resolve(Document original, String base, boolean ignoreMissing, PathResolver pathResolver) throws XIncludeException {
        return new JDOMXIncluder(ignoreMissing, pathResolver).doResolve(original, base);
    }

    public static List<Content> resolve( Element original, String base) throws XIncludeException {
        return new JDOMXIncluder(false, DEFAULT_PATH_RESOLVER).doResolve(original, base);
    }

    private Document doResolve(Document original, String base) {
        if (original == null) {
            throw new NullPointerException("Document must not be null");
        }

        Document result = (Document) original.clone();

        Element root = result.getRootElement();
        List<Content> resolved = doResolve(root, base);

        // check that the list returned contains
        // exactly one root element
        Element newRoot = null;
        Iterator<Content> iterator = resolved.iterator();
        while (iterator.hasNext()) {
            Content o = iterator.next();
            if (o instanceof Element) {
                if (newRoot != null) {
                    throw new XIncludeException("Tried to include multiple roots");
                }
                newRoot = (Element)o;
            }
            else if (o instanceof Comment || o instanceof ProcessingInstruction) {
                // do nothing
            }
            else if (o instanceof Text) {
                throw new XIncludeException("Tried to include text node outside of root element");
            }
            else if (o instanceof EntityRef) {
                throw new XIncludeException("Tried to include a general entity reference outside of root element");
            }
            else {
                throw new XIncludeException("Unexpected type " + o.getClass());
            }

        }

        if (newRoot == null) {
            throw new XIncludeException("No root element");
        }

        // Could probably combine two loops
        List<Content> newContent = result.getContent();
        // resolved contains list of new content
        // use it to replace old root element
        iterator = resolved.iterator();

        // put in nodes before root element
        int rootPosition = newContent.indexOf(result.getRootElement());
        while (iterator.hasNext()) {
            Content o = iterator.next();
            if (o instanceof Comment || o instanceof ProcessingInstruction) {
                newContent.add(rootPosition, o);
                rootPosition++;
            }
            else if (o instanceof Element) { // the root
                break;
            }
            else {
                // throw exception????
            }
        }

        // put in root element
        result.setRootElement(newRoot);

        int addPosition = rootPosition + 1;
        // put in nodes after root element
        while (iterator.hasNext()) {
            Content o = iterator.next();
            if (o instanceof Comment || o instanceof ProcessingInstruction) {
                newContent.add(addPosition, o);
                addPosition++;
            }
            else {
                // throw exception????
            }
        }

        return result;
    }

    private List<Content> doResolve( Element original, String base) throws XIncludeException {
        Stack<String> bases = new Stack<String>();
        if (base != null) bases.push(base);

        List<Content> result = resolve(original, bases);
        bases.pop();
        return result;

    }

    private static boolean isIncludeElement(Element element) {
        return element.getName().equals(INCLUDE) && element.getNamespace().equals(XINCLUDE_NAMESPACE);
    }

    private List<Content> resolve(Element original, Stack<String> bases) throws XIncludeException {
        if (isIncludeElement(original)) {
            return resolveXIncludeElement(original, bases);
        }
        else {
            Element resolvedElement = resolveNonXIncludeElement(original, bases);
            List<Content> resultList = new ArrayList<Content>(1);
            resultList.add(resolvedElement);
            return resultList;
        }
    }

    private List<Content> resolveXIncludeElement(Element element, Stack<String> bases) throws XIncludeException {
        String base = "";
        if (!bases.isEmpty()) base = bases.peek();

        // These lines are probably unnecessary
        assert isIncludeElement(element);

        String href = element.getAttributeValue(HREF);
        assert href != null : "Missing href attribute";

        Attribute baseAttribute = element.getAttribute(BASE, Namespace.XML_NAMESPACE);
        if (baseAttribute != null) {
            base = baseAttribute.getValue();
        }

        URL remote = myPathResolver.resolvePath(href, base);

        boolean parse = true;
        final String parseAttribute = element.getAttributeValue(PARSE);

        if (parseAttribute != null) {
            if (parseAttribute.equals(TEXT)) {
                parse = false;
            }

            assert parseAttribute.equals(XML) : parseAttribute + "is not a legal value for the parse attribute";
        }

        if (parse) {
            assert !bases.contains(remote.toExternalForm()) : "Circular XInclude Reference to " + remote.toExternalForm();

            final Element fallbackElement = element.getChild("fallback", element.getNamespace());
            List<Content> remoteParsed = parseRemote(bases, remote, fallbackElement);
            if (!remoteParsed.isEmpty()) {
                remoteParsed = extractNeededChildren(element, remoteParsed);
            }

            for (int i = 0; i < remoteParsed.size(); i++) {
                Object o = remoteParsed.get(i);

                if (o instanceof Element) {
                    Element e = (Element)o;
                    List<? extends Content> nodes = resolve(e, bases);
                    remoteParsed.addAll(i, nodes);
                    i += nodes.size();
                    remoteParsed.remove(i);
                    i--;
                    e.detach();
                }
            }

            for (Object o : remoteParsed) {
                if (o instanceof Content) {
                    Content content = (Content)o;
                    content.detach();
                }
            }
            return remoteParsed;
        }
        else {
            try {
                String encoding = element.getAttributeValue(ENCODING);
                String s = StreamUtil.readText(URLUtil.openResourceStream(remote), encoding);
                List<Content> resultList = new ArrayList<Content>(1);
                resultList.add(new Text(s));
                return resultList;
            }
            catch (IOException e) {
                throw new XIncludeException(e);
            }
        }

    }

    //xpointer($1)
     public static Pattern XPOINTER_PATTERN = Pattern.compile("xpointer\\((.*)\\)");

    // /$1(/$2)?/*
    public static Pattern CHILDREN_PATTERN = Pattern.compile("/([^/]*)(/[^/]*)?/\\*");

    
    private static List<Content> extractNeededChildren(final Element element, List<Content> remoteElements) {
        final String xpointer = element.getAttributeValue(XPOINTER);
        if (xpointer != null) {

            Matcher matcher = XPOINTER_PATTERN.matcher(xpointer);
            boolean b = matcher.matches();
            assert b : "Unsupported XPointer: " + xpointer;

            String pointer = matcher.group(1);

            matcher = CHILDREN_PATTERN.matcher(pointer);

            b = matcher.matches();
            assert b : "Unsupported pointer: " + pointer;

            final String rootTagName = matcher.group(1);

            assert remoteElements.size() == 1;
            assert remoteElements.get(0) instanceof Element;

            Element e = (Element)remoteElements.get(0);

            if (e.getName().equals(rootTagName)) {
                String subTagName = matcher.group(2);
                if (subTagName != null) {
                    e = e.getChild(subTagName.substring(1));    // cut off the slash
                }
                return new ArrayList<Content>(e.getContent());
            }
            else
                return Collections.emptyList();
        }
        else {
            return remoteElements;
        }
    }

    
    private List<Content> parseRemote(Stack<String> bases,
                                      URL remote,
                                       Element fallbackElement) {
        try {
            Document doc = JDOMUtil.loadResourceDocument(remote);
            bases.push(remote.toExternalForm());

            Element root = doc.getRootElement();

            List<Content> list = resolve(root, bases);

            bases.pop();
            return list;
        }
        catch (JDOMException e) {
            throw new XIncludeException(e);
        }
        catch (IOException e) {
            if (fallbackElement != null) {
                // TODO[yole] return contents of fallback element (we don't have fallback elements with content ATM)
                return Collections.emptyList();
            }
            if (myIgnoreMissing) {
                LOG.info(remote.toExternalForm() + " include ignored: " + e.getMessage());
                return Collections.emptyList();
            }
            throw new XIncludeException(e);
        }
    }

    private Element resolveNonXIncludeElement(Element original, Stack<String> bases) throws XIncludeException {
        Element result = new Element(original.getName(), original.getNamespace());
        for (Attribute a : (List<Attribute>)original.getAttributes()) {
            result.setAttribute((Attribute) a.clone());
        }

        for (Content o : (List<Content>)original.getContent())  {
            if (o instanceof Element) {
                Element element = (Element)o;
                if (isIncludeElement(element)) {
                    result.addContent(resolveXIncludeElement(element, bases));
                }
                else {
                    result.addContent(resolveNonXIncludeElement(element, bases));
                }
            }
            else {
                result.addContent((Content) o.clone());
            }
        }

        return result;
    }

    public interface PathResolver {
        
        URL resolvePath( String relativePath,  String base);
    }
}