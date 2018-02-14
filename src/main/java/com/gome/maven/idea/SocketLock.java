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
package com.gome.maven.idea;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.notification.Notifications;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.net.NetUtils;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author mike
 */
public class SocketLock {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.idea.SocketLock");
    public static final int SOCKET_NUMBER_START = 6942;
    public static final int SOCKET_NUMBER_END = SOCKET_NUMBER_START + 50;

    // IMPORTANT: Some antiviral software detect viruses by the fact of accessing these ports so we should not touch them to appear innocent.
    private static final int[] FORBIDDEN_PORTS = {6953, 6969, 6970};

    private ServerSocket mySocket;
    private final List<String> myLockedPaths = new ArrayList<String>();
    private boolean myIsDialogShown = false;
     private static final String LOCK_THREAD_NAME = "Lock thread";
     private static final String ACTIVATE_COMMAND = "activate ";

    
    private Consumer<List<String>> myActivateListener;
    private final String myToken = UUID.randomUUID().toString();

    public static enum ActivateStatus { ACTIVATED, NO_INSTANCE, CANNOT_ACTIVATE }

    public SocketLock() {
    }

    public void setActivateListener( Consumer<List<String>> consumer) {
        myActivateListener = consumer;
    }

    public synchronized void dispose() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("enter: destroyProcess()");
        }
        try {
            mySocket.close();
            mySocket = null;
        }
        catch (IOException e) {
            LOG.debug(e);
        }
    }

    private volatile int acquiredPort = -1;

    public synchronized int getAcquiredPort () {
        return acquiredPort;
    }

    public ActivateStatus lock(String path, boolean markPort, String... args) {
        return lock(path, path, markPort, args);
    }

    public synchronized ActivateStatus lock(String path, String tokenPath, boolean markPort, String... args) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("enter: lock(path='" + path + "')");
        }

        ActivateStatus status = ActivateStatus.NO_INSTANCE;
        int port = acquireSocket();
        if (mySocket == null) {
            if (!myIsDialogShown) {
                final String productName = ApplicationNamesInfo.getInstance().getProductName();
                if (Main.isHeadless()) { //team server inspections
                    throw new RuntimeException("Only one instance of " + productName + " can be run at a time.");
                }
                 final String pathToLogFile = PathManager.getLogPath() + "/idea.log file".replace('/', File.separatorChar);
                JOptionPane.showMessageDialog(
                        JOptionPane.getRootFrame(),
                        CommonBundle.message("cannot.start.other.instance.is.running.error.message", productName, pathToLogFile),
                        CommonBundle.message("title.warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                myIsDialogShown = true;
            }
            return status;
        }

        if (markPort && port != -1) {
            File portMarker = new File(path, "port");
            try {
                FileUtil.writeToFile(portMarker, Integer.toString(port).getBytes(CharsetToolkit.UTF8_CHARSET));
            }
            catch (IOException ignored) {
                FileUtil.asyncDelete(portMarker);
            }
        }

        File tokenFile = new File(tokenPath, "token");
        String token = "-";
        if (tokenFile.exists()) {
            try {
                token = FileUtil.loadFile(tokenFile);
            }
            catch (IOException ignore) { }
        }

        for (int i = SOCKET_NUMBER_START; i < SOCKET_NUMBER_END; i++) {
            if (isPortForbidden(i) || i == mySocket.getLocalPort()) continue;
            status = tryActivate(i, path, token, args);
            if (status != ActivateStatus.NO_INSTANCE) {
                return status;
            }
        }

        if (!markPort) {
            try {
                FileUtil.writeToFile(tokenFile, myToken.getBytes(CharsetToolkit.UTF8_CHARSET));
                if (SystemInfo.isUnix) {
                    tokenFile.setReadable(false, false);
                    tokenFile.setReadable(true, true);
                    tokenFile.setWritable(false, false);
                    tokenFile.setWritable(true, true);
                }
                tokenFile.deleteOnExit();
            }
            catch (IOException ignored) {
                FileUtil.asyncDelete(tokenFile);
            }
        }

        myLockedPaths.add(path);

        return status;
    }

    public static boolean isPortForbidden(int port) {
        for (int forbiddenPort : FORBIDDEN_PORTS) {
            if (port == forbiddenPort) return true;
        }
        return false;
    }

    private static ActivateStatus tryActivate(int portNumber, String path, String token, String[] args) {
        List<String> result = new ArrayList<String>();

        try {
            try {
                ServerSocket serverSocket = new ServerSocket(portNumber, 50, NetUtils.getLoopbackAddress());
                serverSocket.close();
                return ActivateStatus.NO_INSTANCE;
            }
            catch (IOException ignored) {
            }

            Socket socket = new Socket(NetUtils.getLoopbackAddress(), portNumber);
            socket.setSoTimeout(300);

            DataInputStream in = new DataInputStream(socket.getInputStream());

            while (true) {
                try {
                    result.add(in.readUTF());
                }
                catch (IOException ignored) {
                    break;
                }
            }
            if (result.contains(path)) {
                try {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(ACTIVATE_COMMAND + token + "\0" + new File(".").getAbsolutePath() + "\0" + StringUtil.join(args, "\0"));
                    out.flush();
                    String response = in.readUTF();
                    if (response.equals("ok")) {
                        return ActivateStatus.ACTIVATED;
                    }
                }
                catch(IOException ignored) {
                }
                return ActivateStatus.CANNOT_ACTIVATE;
            }

            in.close();
        }
        catch (IOException e) {
            LOG.debug(e);
        }

        return ActivateStatus.NO_INSTANCE;
    }

    private int acquireSocket() {
        if (mySocket != null) return -1;

        int port = -1;

        for (int i = SOCKET_NUMBER_START; i < SOCKET_NUMBER_END; i++) {
            try {
                if (isPortForbidden(i)) continue;

                mySocket = new ServerSocket(i, 50, InetAddress.getByName("127.0.0.1"));
                port = i;
                acquiredPort = port;
                break;
            }
            catch (IOException e) {
                LOG.info(e);
            }
        }

        final Thread thread = new Thread(new MyRunnable(), LOCK_THREAD_NAME);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        return port;
    }

    private class MyRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        final Socket socket = mySocket.accept();
                        socket.setSoTimeout(800);
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        synchronized (SocketLock.this) {
                            for (String path: myLockedPaths) {
                                out.writeUTF(path);
                            }
                        }
                        DataInputStream stream = new DataInputStream(socket.getInputStream());
                        final String command = stream.readUTF();
                        if (command.startsWith(ACTIVATE_COMMAND)) {
                            if (command.length() <= 8192) {
                                List<String> args = StringUtil.split(command.substring(ACTIVATE_COMMAND.length()), "\0");
                                boolean tokenOK = !args.isEmpty() && myToken.equals(args.get(0));
                                if (!tokenOK) {
                                    LOG.warn("unauthorized request: " + command);
                                    Notifications.Bus.notify(new Notification(
                                            Notifications.SYSTEM_MESSAGES_GROUP_ID,
                                            IdeBundle.message("activation.auth.title"),
                                            IdeBundle.message("activation.auth.message"),
                                            NotificationType.WARNING));
                                }
                                else if (myActivateListener != null) {
                                    myActivateListener.consume(args.subList(1, args.size()));
                                }
                                out.writeUTF("ok");
                            }
                        }
                        out.close();
                    }
                    catch (IOException e) {
                        LOG.debug(e);
                    }
                }
            }
            catch (Throwable ignored) {
            }
        }
    }
}
