package com.wpjava.ui;

import javax.microedition.lcdui.Form;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.App;
import com.wpjava.core.Router;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.SessionStorage;

public class LoadingScreen extends Form implements Runnable {

    private String message;

    public LoadingScreen(String message) {
        super("WPJava");
        this.message = message;
        append(message + "\n\nCargando...");
        new Thread(this).start();
    }

    public void run() {
        try {
            Thread.sleep(700);
        } catch (Exception e) {
        }
        ApiClient api = new ApiClient();
        SessionStorage storage = new SessionStorage();
        if (api.isConnected()) {
            storage.saveToken("backend-connected");
        }
        App.token = storage.getToken();
        App.linked = App.token != null;
        if (App.linked) {
            WPJavaMidlet.getInstance().startSync();
            Router.navigate(new ChatListScreen());
        } else {
            Router.navigate(new QRScreen());
        }
    }
}
