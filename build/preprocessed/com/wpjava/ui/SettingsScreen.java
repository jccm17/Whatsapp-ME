package com.wpjava.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.Config;
import com.wpjava.core.Router;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.StorageManager;

public class SettingsScreen extends Form implements CommandListener {

    private Command backCommand = new Command("Atras", Command.BACK, 1);
    private Command logoutCommand = new Command("Cerrar sesion", Command.SCREEN, 2);
    private Command clearCommand = new Command("Borrar cache", Command.SCREEN, 3);

    public SettingsScreen() {
        super("Ajustes");
        append("Servidor\n" + Config.API_URL + "\n\n");
        append("Sincronizacion\n" + Config.SYNC_INTERVAL + " ms\n\n");
        append("Version\n" + Config.VERSION + "\n\n");
        append("Autor\nJuan Carlos");
        addCommand(backCommand);
        addCommand(logoutCommand);
        addCommand(clearCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            Router.navigate(new ChatListScreen());
        } else if (c == logoutCommand) {
            new ApiClient().logout();
            new StorageManager().clearAll();
            WPJavaMidlet.getInstance().stopSync();
            Router.navigate(new QRScreen());
        } else if (c == clearCommand) {
            new StorageManager().clearAll();
            Router.navigate(new LoadingScreen("Cache borrada"));
        }
    }
}
