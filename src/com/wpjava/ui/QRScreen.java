package com.wpjava.ui;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.App;
import com.wpjava.core.Router;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.SessionStorage;
import com.wpjava.util.ImageUtil;
import com.wpjava.util.JsonParser;

/** Links through the backend's GET /status and GET /qr endpoints. */
public class QRScreen extends Form implements CommandListener {

    private static final int POLL_INTERVAL = 3000;
    /* El N8 tiene pantalla de 360 px de ancho: 260 px permite leer el QR
       cómodamente, dejando margen para el borde y la barra del sistema. */
    private static final int QR_MAX_SIZE = 260;

    private Command linkCommand = new Command("Vincular", Command.OK, 1);
    private Command refreshCommand = new Command("Actualizar", Command.SCREEN, 2);
    private Command exitCommand = new Command("Salir", Command.EXIT, 3);
    private StringItem status = new StringItem("", "Presiona Vincular para conectar WhatsApp.");
    private Timer pollingTimer;
    private boolean checking;
    private boolean completed;
    private boolean qrShown;

    public QRScreen() {
        super("Vincular WhatsApp");
        append("En WhatsApp abre Dispositivos vinculados y selecciona Vincular dispositivo.\n\n");
        append(status);
        addCommand(linkCommand);
        addCommand(refreshCommand);
        addCommand(exitCommand);
        setCommandListener(this);
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == linkCommand) {
            checkConnection();
        } else if (command == refreshCommand) {
            qrShown = false;
            checkConnection();
        } else if (command == exitCommand) {
            stopPolling();
            WPJavaMidlet.getInstance().exit();
        }
    }

    private void checkConnection() {
        if (checking || completed) {
            return;
        }
        checking = true;
        setStatus("Comprobando conexion...");
        new Thread(new Runnable() {
            public void run() {
                String response = new ApiClient().getStatus();
                String linkStatus = JsonParser.getString(response, "status");
                checking = false;

                if ("connected".equals(linkStatus)) {
                    linked();
                } else if ("qr".equals(linkStatus)) {
                    setStatus("QR disponible. Escanealo desde WhatsApp.");
                    if (!qrShown) {
                        loadQr();
                    }
                    startPolling();
                } else if ("connecting".equals(linkStatus)) {
                    setStatus("Generando QR...");
                    startPolling();
                } else if ("disconnected".equals(linkStatus)) {
                    setStatus("El backend esta reconectando. Esperando QR...");
                    startPolling();
                } else {
                    setStatus("No se pudo obtener el estado del backend.");
                }
            }
        }).start();
    }

    private void startPolling() {
        if (pollingTimer != null || completed) {
            return;
        }
        pollingTimer = new Timer();
        pollingTimer.schedule(new TimerTask() {
            public void run() {
                checkConnection();
            }
        }, POLL_INTERVAL, POLL_INTERVAL);
    }

    private void loadQr() {
        new Thread(new Runnable() {
            public void run() {
                byte[] data = new ApiClient().getQR();
                if (data != null && data.length > 0) {
                    showQr(data);
                } else {
                    qrShown = false;
                    setStatus("El QR vencio. Esperando uno nuevo...");
                }
            }
        }).start();
    }

    private void showQr(final byte[] data) {
        display().callSerially(new Runnable() {
            public void run() {
                try {
                    Image image = Image.createImage(data, 0, data.length);
                    image = ImageUtil.scaleToFit(image, QR_MAX_SIZE, QR_MAX_SIZE);
                    qrShown = true;
                    deleteAll();
                    append("Escanea el codigo QR desde WhatsApp.\n\n");
                    append(new ImageItem(null, image, ImageItem.LAYOUT_CENTER, ""));
                    append("\nEsperando la confirmacion del telefono...");
                } catch (Exception e) {
                    status = new StringItem("", "No se pudo abrir el QR recibido.");
                    append(status);
                }
            }
        });
    }

    private void linked() {
        if (completed) {
            return;
        }
        completed = true;
        stopPolling();
        display().callSerially(new Runnable() {
            public void run() {
                String token = "backend-connected";
                new SessionStorage().saveToken(token);
                App.token = token;
                App.linked = true;
                WPJavaMidlet.getInstance().startSync();
                Router.navigate(new ChatListScreen());
            }
        });
    }

    private void setStatus(final String message) {
        display().callSerially(new Runnable() {
            public void run() {
                status.setText(message);
            }
        });
    }

    private void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.cancel();
            pollingTimer = null;
        }
    }

    private Display display() {
        return WPJavaMidlet.getInstance().getDisplay();
    }
}
