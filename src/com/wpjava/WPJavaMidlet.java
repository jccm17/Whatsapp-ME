package com.wpjava;

import java.util.Vector;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

import com.wpjava.core.Router;
import com.wpjava.core.Theme;
import com.wpjava.service.ApiClient;
import com.wpjava.service.SyncService;
import com.wpjava.storage.ChatStorage;
import com.wpjava.storage.PreferenceStorage;
import com.wpjava.storage.Storage;
import com.wpjava.ui.SplashCanvas;

public class WPJavaMidlet extends MIDlet {

    private static WPJavaMidlet instance;
    private Display display;
    private SyncService syncService;
    private boolean started;

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
        if (started) {
            return;
        }
        started = true;
        display = Display.getDisplay(this);
        Theme.init();
        Router.init(this);
        Router.replace(new SplashCanvas());
        startPrefetch();
    }

    /** Loads the cached chat list in the background before the main list opens. */
    private void startPrefetch() {
        if (!new PreferenceStorage().isPermissionAccepted()) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                    Vector chats = new ApiClient().getChats();
                    if (chats != null && chats.size() > 0) {
                        new ChatStorage().saveAll(chats);
                    }
                } catch (Exception ignored) {
                    // The visible screen will load normally; prefetch is optional.
                }
            }
        }).start();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
        stopSync();
        Storage.getInstance().clearAudio();
    }

    public void exit() {
        try {
            destroyApp(true);
        } catch (Exception ignored) {
        }
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
