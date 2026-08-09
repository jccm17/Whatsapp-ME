package com.wpjava.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.Config;
import com.wpjava.core.Router;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.PreferenceStorage;

public class PermissionScreen extends Form implements CommandListener, Runnable {

    private Command allowCommand = new Command("Permitir", Command.OK, 1);
    private Command exitCommand = new Command("Salir", Command.EXIT, 2);

    public PermissionScreen() {
        super("WPJava");
        append("WPJava necesita acceso a Internet para funcionar.\n\n");
        append("Backend: " + Config.API_URL + "\n\n");
        append("Presiona Permitir para probar la conexion.");
        addCommand(allowCommand);
        addCommand(exitCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == allowCommand) {
            deleteAll();
            append("Probando conexion...");
            new Thread(this).start();
        } else if (c == exitCommand) {
            WPJavaMidlet.getInstance().exit();
        }
    }

    public void run() {
        ApiClient api = new ApiClient();
        new PreferenceStorage().savePermissionAccepted();
        if (api.ping()) {
            Router.navigate(new LoadingScreen("Conexion lista"));
        } else {
            Alert alert = new Alert("Error",
                    "No se pudo conectar. Puedes continuar en modo demo.",
                    null, AlertType.WARNING);
            alert.setTimeout(2500);
            Router.getDisplay().setCurrent(alert, new LoadingScreen("Modo demo"));
        }
    }
}
