package com.wpjava.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.Router;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.SessionStorage;
import com.wpjava.util.ImageUtil;

public class QRScreen extends Form implements CommandListener, Runnable {

    private Command linkCommand = new Command("Vincular", Command.OK, 1);
    private Command refreshCommand = new Command("Actualizar", Command.SCREEN, 2);
    private Command exitCommand = new Command("Salir", Command.EXIT, 3);
    private StringItem status;

    public QRScreen() {
        super("Vincular WhatsApp");
        append("Escanea el codigo QR desde WhatsApp.\n\n");
        status = new StringItem("", "Cargando QR...");
        append(status);
        addCommand(linkCommand);
        addCommand(refreshCommand);
        addCommand(exitCommand);
        setCommandListener(this);
        new Thread(this).start();
    }

    public void run() {
        ApiClient api = new ApiClient();
        if (api.isConnected()) {
            new SessionStorage().saveToken("backend-connected");
            WPJavaMidlet.getInstance().startSync();
            Router.navigate(new ChatListScreen());
            return;
        }
        byte[] data = api.getQR();
        if (data != null && data.length > 0) {
            try {
                Image img = Image.createImage(data, 0, data.length);
                img = ImageUtil.scaleToFit(img, 168, 168);
                deleteAll();
                append("Escanea el QR\n");
                append(new ImageItem(null, img, ImageItem.LAYOUT_CENTER, ""));
                append("\nLuego presiona Vincular.");
                return;
            } catch (Exception e) {
            }
        }
        status.setText("No hay QR disponible. Si /status ya dice connected, presiona Actualizar.");
    }

    public void commandAction(Command c, Displayable d) {
        if (c == linkCommand) {
            new SessionStorage().saveToken("demo-token");
            WPJavaMidlet.getInstance().startSync();
            Router.navigate(new ChatListScreen());
        } else if (c == refreshCommand) {
            Router.navigate(new QRScreen());
        } else if (c == exitCommand) {
            WPJavaMidlet.getInstance().exit();
        }
    }
}
