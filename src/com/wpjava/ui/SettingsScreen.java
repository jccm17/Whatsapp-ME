package com.wpjava.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.Config;
import com.wpjava.core.Router;
import com.wpjava.core.Theme;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.StorageManager;
import com.wpjava.storage.Storage;

public class SettingsScreen extends Form implements CommandListener {

    private Command backCommand = new Command("Atras", Command.BACK, 1);
    private Command logoutCommand = new Command("Cerrar sesion", Command.SCREEN, 2);
    private Command clearCommand = new Command("Borrar cache", Command.SCREEN, 3);
    private Command themeCommand = new Command("Cambiar tema", Command.SCREEN, 4);
    private Command serverCommand = new Command("Servidor", Command.SCREEN, 5);
    private Command accessCodeCommand = new Command("Codigo de acceso", Command.SCREEN, 6);
    private Command saveServerCommand = new Command("Guardar", Command.OK, 1);
    private Command cancelServerCommand = new Command("Cancelar", Command.BACK, 2);
    private TextBox serverEditor;
    private TextBox accessCodeEditor;

    public SettingsScreen() {
        super("Ajustes");
        append("Servidor\n" + Storage.getInstance().getServerUrl() + "\n\n");
        append("Codigo de acceso\n" + (Storage.getInstance().getAccessCode().length() > 0
                ? "Configurado" : "No configurado") + "\n\n");
        append("Sincronizacion\n" + Config.SYNC_INTERVAL + " ms\n\n");
        append("Version\n" + Config.VERSION + "\n\n");
        append("Autor\nJuan Carlos");
        addCommand(backCommand);
        addCommand(logoutCommand);
        addCommand(clearCommand);
        addCommand(themeCommand);
        addCommand(serverCommand);
        addCommand(accessCodeCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (d == serverEditor) {
            if (c == saveServerCommand) {
                String value = normalizeServer(serverEditor.getString());
                if (value.length() > 0) {
                    Storage.getInstance().setServerUrl(value);
                    Storage.getInstance().save();
                }
            }
            serverEditor = null;
            Router.replace(new SettingsScreen());
        } else if (d == accessCodeEditor) {
            if (c == saveServerCommand) {
                Storage.getInstance().setAccessCode(accessCodeEditor.getString().trim());
                Storage.getInstance().save();
            }
            accessCodeEditor = null;
            Router.replace(new SettingsScreen());
        } else if (c == backCommand) {
            Router.navigate(new ChatListScreen());
        } else if (c == logoutCommand) {
            new ApiClient().logout();
            new StorageManager().clearAll();
            WPJavaMidlet.getInstance().stopSync();
            Router.navigate(new QRScreen());
        } else if (c == clearCommand) {
            new StorageManager().clearAll();
            Router.navigate(new LoadingScreen("Cache borrada"));
        } else if (c == themeCommand) {
            Theme.setDark(!Theme.isDark());
            Router.replace(new SettingsScreen());
        } else if (c == serverCommand) {
            editServer();
        } else if (c == accessCodeCommand) {
            editAccessCode();
        }
    }

    private void editServer() {
        serverEditor = new TextBox("Servidor HTTP", Storage.getInstance().getServerUrl(),
                120, TextField.URL);
        serverEditor.addCommand(saveServerCommand);
        serverEditor.addCommand(cancelServerCommand);
        serverEditor.setCommandListener(this);
        Router.getDisplay().setCurrent(serverEditor);
    }

    private void editAccessCode() {
        accessCodeEditor = new TextBox("Codigo de acceso", Storage.getInstance().getAccessCode(),
                128, TextField.PASSWORD);
        accessCodeEditor.addCommand(saveServerCommand);
        accessCodeEditor.addCommand(cancelServerCommand);
        accessCodeEditor.setCommandListener(this);
        Router.getDisplay().setCurrent(accessCodeEditor);
    }

    private String normalizeServer(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.length() == 0) return "";
        if (value.indexOf("http://") != 0 && value.indexOf("https://") != 0) {
            value = "http://" + value;
        }
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
