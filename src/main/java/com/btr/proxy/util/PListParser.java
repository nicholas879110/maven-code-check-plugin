//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.btr.proxy.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class PListParser {
    private static final PListParser PLIST = new PListParser();
    private static final String BASE64_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final char[] BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private final DateFormat m_dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final Map<Class<?>, PListParser.ElementType> m_simpleTypes;

    static void silentlyClose(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException var2) {
            ;
        }

    }

    private static PListParser.Dict parse(InputSource input) throws PListParser.XmlParseException {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            documentBuilder.setEntityResolver(new EmptyXMLResolver());
            Document doc = documentBuilder.parse(input);
            Element element = doc.getDocumentElement();
            return PLIST.parse(element);
        } catch (ParserConfigurationException var4) {
            throw new PListParser.XmlParseException("Error reading input", var4);
        } catch (SAXException var5) {
            throw new PListParser.XmlParseException("Error reading input", var5);
        } catch (IOException var6) {
            throw new PListParser.XmlParseException("Error reading input", var6);
        }
    }

    public static PListParser.Dict load(File file) throws PListParser.XmlParseException, IOException {
        FileInputStream byteStream = new FileInputStream(file);

        PListParser.Dict var3;
        try {
            InputSource input = new InputSource(byteStream);
            var3 = parse(input);
        } finally {
            silentlyClose(byteStream);
        }

        return var3;
    }

    PListParser() {
        this.m_dateFormat.setTimeZone(TimeZone.getTimeZone("Z"));
        this.m_simpleTypes = new HashMap();
        this.m_simpleTypes.put(Integer.class, PListParser.ElementType.INTEGER);
        this.m_simpleTypes.put(Byte.class, PListParser.ElementType.INTEGER);
        this.m_simpleTypes.put(Short.class, PListParser.ElementType.INTEGER);
        this.m_simpleTypes.put(Short.class, PListParser.ElementType.INTEGER);
        this.m_simpleTypes.put(Long.class, PListParser.ElementType.INTEGER);
        this.m_simpleTypes.put(String.class, PListParser.ElementType.STRING);
        this.m_simpleTypes.put(Float.class, PListParser.ElementType.REAL);
        this.m_simpleTypes.put(Double.class, PListParser.ElementType.REAL);
        this.m_simpleTypes.put(byte[].class, PListParser.ElementType.DATA);
        this.m_simpleTypes.put(Boolean.class, PListParser.ElementType.TRUE);
        this.m_simpleTypes.put(Date.class, PListParser.ElementType.DATE);
    }

    PListParser.Dict parse(Element element) throws PListParser.XmlParseException {
        if (!"plist".equalsIgnoreCase(element.getNodeName())) {
            throw new PListParser.XmlParseException("Expected plist top element, was: " + element.getNodeName());
        } else {
            Node n;
            for(n = element.getFirstChild(); n != null && !n.getNodeName().equals("dict"); n = n.getNextSibling()) {
                ;
            }

            PListParser.Dict result = (PListParser.Dict)this.parseElement(n);
            return result;
        }
    }

    private Object parseElement(Node element) throws PListParser.XmlParseException {
        try {
            return this.parseElementRaw(element);
        } catch (Exception var3) {
            throw new PListParser.XmlParseException("Failed to parse: " + element.getNodeName(), var3);
        }
    }

    private Object parseElementRaw(Node element) throws ParseException {
        PListParser.ElementType type = PListParser.ElementType.valueOf(element.getNodeName().toUpperCase());
        switch(type) {
            case INTEGER:
                return this.parseInt(this.getValue(element));
            case REAL:
                return Double.valueOf(this.getValue(element));
            case STRING:
                return this.getValue(element);
            case DATE:
                return this.m_dateFormat.parse(this.getValue(element));
            case DATA:
                return base64decode(this.getValue(element));
            case ARRAY:
                return this.parseArray(element.getChildNodes());
            case TRUE:
                return Boolean.TRUE;
            case FALSE:
                return Boolean.FALSE;
            case DICT:
                return this.parseDict(element.getChildNodes());
            default:
                throw new RuntimeException("Unexpected type: " + element.getNodeName());
        }
    }

    private String getValue(Node n) {
        StringBuilder sb = new StringBuilder();

        for(Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() == 3) {
                sb.append(c.getNodeValue());
            }
        }

        return sb.toString();
    }

    private Number parseInt(String value) {
        Long l = Long.valueOf(value);
        return (Number)((long)l.intValue() == l.longValue() ? l.intValue() : l);
    }

    private PListParser.Dict parseDict(NodeList elements) throws ParseException {
        PListParser.Dict dict = new PListParser.Dict();

        for(int i = 0; i < elements.getLength(); ++i) {
            Node key = elements.item(i);
            if (key.getNodeType() == 1) {
                if (!"key".equals(key.getNodeName())) {
                    throw new ParseException("Expected key but was " + key.getNodeName(), -1);
                }

                ++i;

                Node value;
                for(value = elements.item(i); value.getNodeType() != 1; value = elements.item(i)) {
                    ++i;
                }

                Object o = this.parseElementRaw(value);
                String dictName = this.getValue(key);
                dict.children.put(dictName, o);
            }
        }

        return dict;
    }

    private List<Object> parseArray(NodeList elements) throws ParseException {
        ArrayList<Object> list = new ArrayList();

        for(int i = 0; i < elements.getLength(); ++i) {
            Node o = elements.item(i);
            if (o.getNodeType() == 1) {
                list.add(this.parseElementRaw(o));
            }
        }

        return list;
    }

    static String base64encode(byte[] bytes) {
        StringBuilder builder = new StringBuilder((bytes.length + 2) / 3 * 4);

        for(int i = 0; i < bytes.length; i += 3) {
            byte b0 = bytes[i];
            byte b1 = i < bytes.length - 1 ? bytes[i + 1] : 0;
            byte b2 = i < bytes.length - 2 ? bytes[i + 2] : 0;
            builder.append(BASE64_CHARS[(b0 & 255) >> 2]);
            builder.append(BASE64_CHARS[(b0 & 3) << 4 | (b1 & 240) >> 4]);
            builder.append(i < bytes.length - 1 ? BASE64_CHARS[(b1 & 15) << 2 | (b2 & 192) >> 6] : "=");
            builder.append(i < bytes.length - 2 ? BASE64_CHARS[b2 & 63] : "=");
        }

        return builder.toString();
    }

    static byte[] base64decode(String base64) {
        base64 = base64.trim();
        int endTrim = base64.endsWith("==") ? 2 : (base64.endsWith("=") ? 1 : 0);
        int length = base64.length() / 4 * 3 - endTrim;
        base64 = base64.replace('=', 'A');
        byte[] result = new byte[length];
        int stringLength = base64.length();
        int index = 0;

        for(int i = 0; i < stringLength; i += 4) {
            int i0 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(base64.charAt(i));
            int i1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(base64.charAt(i + 1));
            int i2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(base64.charAt(i + 2));
            int i3 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".indexOf(base64.charAt(i + 3));
            byte b0 = (byte)(i0 << 2 | i1 >> 4);
            byte b1 = (byte)(i1 << 4 | i2 >> 2);
            byte b2 = (byte)(i2 << 6 | i3);
            result[index++] = b0;
            if (index < length) {
                result[index++] = b1;
                if (index < length) {
                    result[index++] = b2;
                }
            }
        }

        return result;
    }

    private static enum ElementType {
        INTEGER,
        STRING,
        REAL,
        DATA,
        DATE,
        DICT,
        ARRAY,
        TRUE,
        FALSE;

        private ElementType() {
        }
    }

    public static class Dict implements Iterable<Entry<String, Object>> {
        private Map<String, Object> children = new HashMap();

        public Dict() {
        }

        public Object get(String key) {
            return this.children.get(key);
        }

        public Iterator<Entry<String, Object>> iterator() {
            return this.children.entrySet().iterator();
        }

        public int size() {
            return this.children.size();
        }

        public void dump() {
            System.out.println("PList");
            dumpInternal(this, 1);
        }

        private static void dumpInternal(PListParser.Dict plist, int indent) {
            Iterator i$ = plist.iterator();

            while(true) {
                while(i$.hasNext()) {
                    Entry<String, Object> child = (Entry)i$.next();
                    int j;
                    if (child.getValue() instanceof PListParser.Dict) {
                        for(j = 0; j < indent; ++j) {
                            System.out.print("  ");
                        }

                        System.out.println((String)child.getKey());
                        dumpInternal((PListParser.Dict)child.getValue(), indent + 1);
                    } else {
                        for(j = 0; j < indent; ++j) {
                            System.out.print("  ");
                        }

                        System.out.println((String)child.getKey() + " = " + child.getValue());
                    }
                }

                return;
            }
        }

        public Object getAtPath(String path) {
            PListParser.Dict currentNode = this;
            String[] pathSegments = path.trim().split("/");

            for(int i = 0; i < pathSegments.length; ++i) {
                String segment = pathSegments[i].trim();
                if (segment.length() != 0) {
                    Object o = currentNode.get(segment);
                    if (i >= pathSegments.length - 1) {
                        return o;
                    }

                    if (o == null || !(o instanceof PListParser.Dict)) {
                        break;
                    }

                    currentNode = (PListParser.Dict)o;
                }
            }

            return null;
        }
    }

    public static class XmlParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public XmlParseException() {
        }

        public XmlParseException(String msg) {
            super(msg);
        }

        public XmlParseException(String msg, Exception e) {
            super(msg, e);
        }
    }
}
