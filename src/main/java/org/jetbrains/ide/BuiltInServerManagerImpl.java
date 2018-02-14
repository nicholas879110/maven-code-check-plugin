//package org.jetbrains.ide;
//
//import com.gome.maven.notification.NotificationDisplayType;
//import com.gome.maven.notification.NotificationGroup;
//import com.gome.maven.notification.NotificationType;
//import com.gome.maven.openapi.Disposable;
//import com.gome.maven.openapi.application.ApplicationManager;
//import com.gome.maven.openapi.application.ApplicationNamesInfo;
//import com.gome.maven.openapi.diagnostic.Logger;
//import com.gome.maven.openapi.util.Disposer;
//import com.gome.maven.openapi.util.NotNullLazyValue;
//import com.gome.maven.openapi.util.ShutDownTracker;
//import com.gome.maven.openapi.util.text.StringUtil;
//import com.gome.maven.util.Url;
//import com.gome.maven.util.net.NetUtils;
//import org.jetbrains.builtInWebServer.BuiltInServerOptions;
//import org.jetbrains.io.BuiltInServer;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Future;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//public class BuiltInServerManagerImpl extends BuiltInServerManager {
//    private static final Logger LOG = Logger.getInstance(BuiltInServerManager.class);
//
//    public static final NotNullLazyValue<NotificationGroup> NOTIFICATION_GROUP = new NotNullLazyValue<NotificationGroup>() {
//
//        @Override
//        protected NotificationGroup compute() {
//            return new NotificationGroup("Built-in Server", NotificationDisplayType.STICKY_BALLOON, true);
//        }
//    };
//
//
//    public static final String PROPERTY_RPC_PORT = "rpc.port";
//    private static final int PORTS_COUNT = 20;
//
//    private volatile int detectedPortNumber = -1;
//    private final AtomicBoolean started = new AtomicBoolean(false);
//
//
//    private BuiltInServer server;
//
//    @Override
//    public int getPort() {
//        return detectedPortNumber == -1 ? getDefaultPort() : detectedPortNumber;
//    }
//
//    @Override
//    public BuiltInServerManager waitForStart() {
//        Future<?> serverStartFuture = startServerInPooledThread();
//        if (serverStartFuture != null) {
//            LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode() || !ApplicationManager.getApplication().isDispatchThread());
//            try {
//                serverStartFuture.get();
//            }
//            catch (InterruptedException ignored) {
//            }
//            catch (ExecutionException ignored) {
//            }
//        }
//        return this;
//    }
//
//    private static int getDefaultPort() {
//        if (System.getProperty(PROPERTY_RPC_PORT) == null) {
//            // Default port will be occupied by main idea instance - define the custom default to avoid searching of free port
//            return ApplicationManager.getApplication().isUnitTestMode() ? 64463 : 63342;
//        }
//        else {
//            return Integer.parseInt(System.getProperty(PROPERTY_RPC_PORT));
//        }
//    }
//
//    @Override
//    public void initComponent() {
//        startServerInPooledThread();
//    }
//
//    private Future<?> startServerInPooledThread() {
//        if (!started.compareAndSet(false, true)) {
//            return null;
//        }
//
//        return ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
//            @Override
//            public void run() {
//                int defaultPort = getDefaultPort();
//                int workerCount = 1;
//                // if user set special port number for some service (eg built-in web server), we should slightly increase worker count
//                if (Runtime.getRuntime().availableProcessors() > 1) {
//                    for (CustomPortServerManager customPortServerManager : CustomPortServerManager.EP_NAME.getExtensions()) {
//                        if (customPortServerManager.getPort() != defaultPort) {
//                            workerCount = 2;
//                            break;
//                        }
//                    }
//                }
//
//                try {
//                    server = new BuiltInServer();
//                    detectedPortNumber = server.start(workerCount, defaultPort, PORTS_COUNT, true);
//                }
//                catch (Exception e) {
//                    LOG.info(e);
//                    NOTIFICATION_GROUP.getValue().createNotification(
//                            "Cannot start internal HTTP server. Git integration, JavaScript debugger and LiveEdit may operate with errors. " +
//                                    "Please check your firewall settings and restart " + ApplicationNamesInfo.getInstance().getFullProductName(),
//                            NotificationType.ERROR).notify(null);
//                    return;
//                }
//
//                if (detectedPortNumber == -1) {
//                    LOG.info("built-in server cannot be started, cannot bind to port");
//                    return;
//                }
//
//                LOG.info("built-in server started, port " + detectedPortNumber);
//
//                Disposer.register(ApplicationManager.getApplication(), server);
//                ShutDownTracker.getInstance().registerShutdownTask(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (!Disposer.isDisposed(server)) {
//                            // something went wrong
//                            Disposer.dispose(server);
//                        }
//                    }
//                });
//            }
//        });
//    }
//
//    @Override
//
//    public Disposable getServerDisposable() {
//        return server;
//    }
//
//    @Override
//    public boolean isOnBuiltInWebServer( Url url) {
//        return url != null && !StringUtil.isEmpty(url.getAuthority()) && isOnBuiltInWebServerByAuthority(url.getAuthority());
//    }
//
//    private static boolean isOnBuiltInWebServerByAuthority( String authority) {
//        int portIndex = authority.indexOf(':');
//        if (portIndex < 0 || portIndex == authority.length() - 1) {
//            return false;
//        }
//
//        int port = StringUtil.parseInt(authority.substring(portIndex + 1), -1);
//        if (port == -1) {
//            return false;
//        }
//
//        BuiltInServerOptions options = BuiltInServerOptions.getInstance();
//        int idePort = BuiltInServerManager.getInstance().getPort();
//        if (options.builtInServerPort != port && idePort != port) {
//            return false;
//        }
//
//        String host = authority.substring(0, portIndex);
//        if (NetUtils.isLocalhost(host)) {
//            return true;
//        }
//
//        try {
//            InetAddress inetAddress = InetAddress.getByName(host);
//            return inetAddress.isLoopbackAddress() ||
//                    inetAddress.isAnyLocalAddress() ||
//                    (options.builtInServerAvailableExternally && idePort != port && NetworkInterface.getByInetAddress(inetAddress) != null);
//        }
//        catch (IOException e) {
//            return false;
//        }
//    }
//}