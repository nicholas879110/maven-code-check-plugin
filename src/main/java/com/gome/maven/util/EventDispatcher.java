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

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.util.containers.ContainerUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventListener;
import java.util.List;

/**
 * @author max
 */
public class EventDispatcher<T extends EventListener> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.util.EventDispatcher");

    private final T myMulticaster;

    private final List<T> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    public static <T extends EventListener> EventDispatcher<T> create( Class<T> listenerClass) {
        return new EventDispatcher<T>(listenerClass);
    }

    private EventDispatcher( Class<T> listenerClass) {
        LOG.assertTrue(listenerClass.isInterface(), "listenerClass must be an interface");
        InvocationHandler handler = new InvocationHandler() {
            @Override
            
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getDeclaringClass().getName().equals("java.lang.Object")) {
                     String methodName = method.getName();
                    if (methodName.equals("toString")) {
                        return "Multicaster";
                    }
                    else if (methodName.equals("hashCode")) {
                        return Integer.valueOf(System.identityHashCode(proxy));
                    }
                    else if (methodName.equals("equals")) {
                        return proxy == args[0] ? Boolean.TRUE : Boolean.FALSE;
                    }
                    else {
                        LOG.error("Incorrect Object's method invoked for proxy:" + methodName);
                        return null;
                    }
                }
                else {
                    dispatch(method, args);
                    return null;
                }
            }
        };

        //noinspection unchecked
        myMulticaster = (T)Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, handler);
    }

    
    public T getMulticaster() {
        return myMulticaster;
    }

    private void dispatch( Method method, Object[] args) {
        method.setAccessible(true);

        for (T listener : myListeners) {
            try {
                method.invoke(listener, args);
            }
            catch (AbstractMethodError ignored) {
                // Do nothing. This listener just does not implement something newly added yet.
                // AbstractMethodError is normally wrapped in InvocationTargetException,
                // but some Java versions didn't do it in some cases (see http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6531596)
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                final Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                }
                else if (!(cause instanceof AbstractMethodError)) { // AbstractMethodError means this listener doesn't implement some new method in interface
                    LOG.error(cause);
                }
            }
        }
    }

    public void addListener( T listener) {
        myListeners.add(listener);
    }

    public void addListener( final T listener,  Disposable parentDisposable) {
        addListener(listener);
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                removeListener(listener);
            }
        });
    }

    public void removeListener( T listener) {
        myListeners.remove(listener);
    }

    public boolean hasListeners() {
        return !myListeners.isEmpty();
    }

    
    public List<T> getListeners() {
        return myListeners;
    }
}
