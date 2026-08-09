package com.wpjava;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

import com.wpjava.core.Router;
import com.wpjava.service.SyncService;
import com.wpjava.ui.SplashCanvas;

public class WPJavaMidlet extends MIDlet {

    private static WPJavaMidlet instance;
    private Display display;
    private SyncService syncService;

    public WPJavaMidlet() {
        instance = this;
    }

    public static WPJavaMidlet getInstance() {
        return instance;
    }

    public Display getDisplay() {
        return display;
    }

    public void startApp() {
        display = Display.getDisplay(this);
        Router.init(display);
        display.setCurrent(new SplashCanvas());
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
        stopSync();
    }

    public void exit() {
        stopSync();
        notifyDestroyed();
    }

    public void startSync() {
        if (syncService == null) {
            syncService = new SyncService();
            syncService.start();
        }
    }

    public void stopSync() {
        if (syncService != null) {
            syncService.stop();
            syncService = null;
        }
    }
}
