package com.wpjava.service;

import com.wpjava.core.Config;

public class SyncService implements Runnable {

    private boolean running;
    private Thread thread;
    private ApiClient api;

    public SyncService() {
        api = new ApiClient();
    }

    public void start() {
        if (thread == null) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        running = false;
        thread = null;
    }

    public void run() {
        while (running) {
            try {
                api.sync();
                Thread.sleep(Config.SYNC_INTERVAL);
            } catch (Exception e) {
            }
        }
    }
}
