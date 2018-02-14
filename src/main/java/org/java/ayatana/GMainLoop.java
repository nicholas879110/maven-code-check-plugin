//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.java.ayatana;

class GMainLoop {
    private static boolean running = false;

    GMainLoop() {
    }

    private static native void runGMainLoop();

    private static native void quitGMainLoop();

    public static synchronized void run() {
        if (!running) {
            runGMainLoop();
            Thread threadQuit = new Thread() {
                public void run() {
                    GMainLoop.quitGMainLoop();
                }
            };
            threadQuit.setDaemon(true);
            Runtime.getRuntime().addShutdownHook(threadQuit);
            running = true;
        }

    }
}
