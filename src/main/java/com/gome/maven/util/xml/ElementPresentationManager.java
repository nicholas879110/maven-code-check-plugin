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
package com.gome.maven.util.xml;

import com.gome.maven.ide.IconProvider;
import com.gome.maven.ide.TypePresentationService;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.util.Function;
import com.gome.maven.util.NullableFunction;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.util.containers.ConcurrentFactoryMap;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author peter
 */
public abstract class ElementPresentationManager {
    private static final ConcurrentFactoryMap<Class,Method> ourNameValueMethods = new ConcurrentFactoryMap<Class, Method>() {
        @Override
        
        protected Method create(final Class key) {
            for (final Method method : ReflectionUtil.getClassPublicMethods(key)) {
                if (JavaMethod.getMethod(key, method).getAnnotation(NameValue.class) != null) {
                    return method;
                }
            }
            return null;
        }
    };

    private final static Function<Object, String> DEFAULT_NAMER = new Function<Object, String>() {
        @Override
        
        public String fun(final Object element) {
            return getElementName(element);
        }
    };

    public static ElementPresentationManager getInstance() {
        return ServiceManager.getService(ElementPresentationManager.class);
    }

    
    public <T> Object[] createVariants(Collection<T> elements) {
        return createVariants(elements, (Function<T, String>)DEFAULT_NAMER);
    }

    
    public <T> Object[] createVariants(Collection<T> elements, int iconFlags) {
        return createVariants(elements, (Function<T, String>)DEFAULT_NAMER, iconFlags);
    }

    
    public <T> Object[] createVariants(Collection<T> elements, Function<T, String> namer) {
        return createVariants(elements, namer, 0);
    }

    /**
     * Use {@link com.gome.maven.codeInsight.lookup.LookupElementBuilder}
     */
    @Deprecated
    public abstract Object createVariant(final Object variant, final String name, final PsiElement psiElement);

    
    public abstract <T> Object[] createVariants(Collection<T> elements, Function<T, String> namer, int iconFlags);


    private static final List<Function<Object, String>> ourNameProviders = new ArrayList<Function<Object, String>>();
    private static final List<Function<Object, String>> ourDocumentationProviders = new ArrayList<Function<Object, String>>();
    private static final List<Function<Object, Icon>> ourIconProviders = new ArrayList<Function<Object, Icon>>();

    static {
        ourIconProviders.add(new NullableFunction<Object, Icon>() {
            @Override
            public Icon fun(final Object o) {
                return o instanceof Iconable ? ((Iconable)o).getIcon(Iconable.ICON_FLAG_READ_STATUS) : null;
            }
        });
    }

    /**
     * @deprecated
     * @see com.gome.maven.ide.presentation.Presentation#provider()
     */
    public static void registerNameProvider(Function<Object, String> function) { ourNameProviders.add(function); }

    /**
     * @deprecated
     * @see Documentation
     */
    public static void registerDocumentationProvider(Function<Object, String> function) { ourDocumentationProviders.add(function); }


    public static <T>NullableFunction<T, String> NAMER() {
        return new NullableFunction<T, String>() {
            @Override
            public String fun(final T o) {
                return getElementName(o);
            }
        };
    }

    public static final NullableFunction<Object, String> NAMER = new NullableFunction<Object, String>() {
        @Override
        public String fun(final Object o) {
            return getElementName(o);
        }
    };
    public static <T> NullableFunction<T, String> namer() {
        //noinspection unchecked
        return (NullableFunction<T, String>)NAMER;
    }

    
    public static String getElementName( Object element) {
        for (final Function<Object, String> function : ourNameProviders) {
            final String s = function.fun(element);
            if (s != null) {
                return s;
            }
        }
        Object o = invokeNameValueMethod(element);
        if (o == null || o instanceof String) return (String)o;
        if (o instanceof GenericValue) {
            final GenericValue gv = (GenericValue)o;
            final String s = gv.getStringValue();
            if (s == null) {
                final Object value = gv.getValue();
                if (value != null) {
                    return String.valueOf(value);
                }
            }
            return s;
        }
        return null;
    }

    
    public static String getDocumentationForElement(Object element) {
        for (final Function<Object, String> function : ourDocumentationProviders) {
            final String s = function.fun(element);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    
    public static Object invokeNameValueMethod( final Object element) {
        final Method nameValueMethod = findNameValueMethod(element.getClass());
        if (nameValueMethod == null) {
            return null;
        }

        return DomReflectionUtil.invokeMethod(nameValueMethod, element);
    }

    public static String getTypeNameForObject(Object o) {
        final Object firstImpl = ModelMergerUtil.getFirstImplementation(o);
        o = firstImpl != null ? firstImpl : o;
        String typeName = TypePresentationService.getService().getTypeName(o);
        if (typeName != null) return typeName;
        if (o instanceof DomElement) {
            final DomElement element = (DomElement)o;
            return StringUtil.capitalizeWords(element.getNameStrategy().splitIntoWords(element.getXmlElementName()), true);
        }
        return TypePresentationService.getDefaultTypeName(o.getClass());
    }

    public static Icon getIcon( Object o) {
        for (final Function<Object, Icon> function : ourIconProviders) {
            final Icon icon = function.fun(o);
            if (icon != null) {
                return icon;
            }
        }
        if (o instanceof DomElement) {
            final DomElement domElement = (DomElement)o;
            final boolean dumb = DumbService.getInstance(domElement.getManager().getProject()).isDumb();

            for (final IconProvider provider : IconProvider.EXTENSION_POINT_NAME.getExtensions()) {
                if (provider instanceof DomIconProvider) {
                    if (dumb && !DumbService.isDumbAware(provider)) {
                        continue;
                    }

                    final Icon icon = ((DomIconProvider)provider).getIcon(domElement, 0);
                    if (icon != null) {
                        return icon;
                    }
                }
            }
        }

        final Icon[] icons = getIconsForClass(o.getClass(), o);
        if (icons != null && icons.length > 0) {
            return icons[0];
        }
        return null;
    }

    
    public static Icon getIconOld(Object o) {
        for (final Function<Object, Icon> function : ourIconProviders) {
            final Icon icon = function.fun(o);
            if (icon != null) {
                return icon;
            }
        }
        final Icon[] icons = getIconsForClass(o.getClass(), o);
        if (icons != null && icons.length > 0) {
            return icons[0];
        }
        return null;
    }

    
    private static <T> T getFirst( final T[] array) {
        return array == null || array.length == 0 ? null : array[0];
    }


    
    public static Icon getIconForClass(Class clazz) {
        return getFirst(getIconsForClass(clazz, null));
    }

    
    private static Icon[] getIconsForClass(final Class clazz,  Object o) {
        TypePresentationService service = TypePresentationService.getService();
        final Icon icon = o == null ? service.getTypeIcon(clazz) : service.getIcon(o);
        if (icon != null) {
            return new Icon[]{icon};
        }

        return null;
    }

    public static Method findNameValueMethod(final Class<?> aClass) {
        synchronized (ourNameValueMethods) {
            return ourNameValueMethods.get(aClass);
        }
    }

    
    public static <T> T findByName(Collection<T> collection, final String name) {
        return ContainerUtil.find(collection, new Condition<T>() {
            @Override
            public boolean value(final T object) {
                return Comparing.equal(name, getElementName(object), true);
            }
        });
    }

}
