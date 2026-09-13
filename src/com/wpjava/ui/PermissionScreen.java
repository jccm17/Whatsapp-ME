package com.wpjava.ui;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.Router;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.PreferenceStorage;
import com.wpjava.storage.Storage;

/** Full-screen network permission and connection test shown on first launch. */
public class PermissionScreen extends Canvas implements CommandListener {

    private static final int COLOR_GREEN = 0x075E84;
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_BLACK = 0x000000;
    private static final int COLOR_GRAY = 0x888888;
    private static final int COLOR_LIGHT_GRAY = 0xF0F0F0;
    private static final int COLOR_RED = 0xCC0000;

    private String errorMessage;
    private boolean testing;
    private int allowX;
    private int allowY;
    private int exitX;
    private int exitY;
    private int configX;
    private int configY;
    private int buttonWidth = 90;
    private int buttonHeight = 44;
    private Command saveCommand = new Command("Guardar", Command.OK, 1);
    private Command cancelCommand = new Command("Cancelar", Command.BACK, 2);
    private TextBox serverEditor;
    private TextBox accessCodeEditor;

    public PermissionScreen() {
        setFullScreenMode(true);
    }

    protected void paint(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        g.setColor(COLOR_WHITE);
        g.fillRect(0, 0, width, height);
        drawBrand(g, width, height);

        g.setColor(COLOR_GRAY);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        drawWrapped(g, "Configura el servidor y codigo antes de permitir el acceso a Internet.",
                20, height / 3 + 20, width - 40);
        drawWrapped(g, "Backend: " + Storage.getInstance().getServerUrl(),
                20, height / 3 + 48, width - 40);

        if (errorMessage != null) {
            g.setColor(COLOR_RED);
            drawWrapped(g, errorMessage, 20, height / 2, width - 40);
        }
        if (testing) {
            g.setColor(COLOR_GRAY);
            g.drawString("Probando conexion...", width / 2, height - 82,
                    Graphics.HCENTER | Graphics.TOP);
        }

        configX = width / 2 - 60;
        configY = height - 118;
        drawButton(g, "Configurar", configX, configY, 120, buttonHeight,
                0x455A64, COLOR_WHITE);

        int buttonY = height - 65;
        allowX = width / 2 - 100;
        allowY = buttonY;
        exitX = width / 2 + 10;
        exitY = buttonY;
        drawButton(g, "Permitir", allowX, allowY, buttonWidth, buttonHeight,
                COLOR_GREEN, COLOR_WHITE);
        drawButton(g, "Salir", exitX, exitY, buttonWidth, buttonHeight,
                0xE0E0E0, COLOR_BLACK);
        drawSoftkeys(g, width, height);
    }

    protected void pointerPressed(int x, int y) {
        if (hit(x, y, allowX, allowY, buttonWidth, buttonHeight)) {
            handleAllow();
        } else if (hit(x, y, exitX, exitY, buttonWidth, buttonHeight)) {
            WPJavaMidlet.getInstance().exit();
        } else if (hit(x, y, configX, configY, 120, buttonHeight)) {
            editServer();
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (keyCode == -6 || action == FIRE) {
            handleAllow();
        } else if (keyCode == -7) {
            WPJavaMidlet.getInstance().exit();
        } else if (keyCode == -11) {
            editServer();
        }
    }

    public void commandAction(Command command, Displayable displayable) {
        if (displayable == serverEditor) {
            if (command == saveCommand) {
                String value = normalizeServer(serverEditor.getString());
                if (value.length() > 0) {
                    Storage.getInstance().setServerUrl(value);
                }
                editAccessCode();
            } else {
                closeEditors();
            }
        } else if (displayable == accessCodeEditor) {
            if (command == saveCommand) {
                Storage.getInstance().setAccessCode(accessCodeEditor.getString().trim());
                Storage.getInstance().save();
            }
            closeEditors();
        }
    }

    private void editServer() {
        serverEditor = new TextBox("Servidor HTTPS", Storage.getInstance().getServerUrl(),
                120, TextField.URL);
        serverEditor.addCommand(saveCommand);
        serverEditor.addCommand(cancelCommand);
        serverEditor.setCommandListener(this);
        Display.getDisplay(WPJavaMidlet.getInstance()).setCurrent(serverEditor);
    }

    private void editAccessCode() {
        accessCodeEditor = new TextBox("Codigo de acceso", Storage.getInstance().getAccessCode(),
                128, TextField.PASSWORD);
        accessCodeEditor.addCommand(saveCommand);
        accessCodeEditor.addCommand(cancelCommand);
        accessCodeEditor.setCommandListener(this);
        Display.getDisplay(WPJavaMidlet.getInstance()).setCurrent(accessCodeEditor);
    }

    private void closeEditors() {
        serverEditor = null;
        accessCodeEditor = null;
        Router.replace(new PermissionScreen());
    }

    private String normalizeServer(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.length() == 0) return "";
        if (value.indexOf("http://") != 0 && value.indexOf("https://") != 0) {
            value = "https://" + value;
        }
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private void handleAllow() {
        if (testing) return;
        testing = true;
        errorMessage = null;
        repaint();
        new Thread(new Runnable() {
            public void run() {
                final boolean connected = new ApiClient().ping();
                Display.getDisplay(WPJavaMidlet.getInstance()).callSerially(new Runnable() {
                    public void run() {
                        testing = false;
                        if (connected) {
                            new PreferenceStorage().savePermissionAccepted();
                            Router.navigate(new LoadingScreen("Conexion lista"));
                        } else {
                            errorMessage = "No se pudo conectar al backend. Revisa la red y vuelve a intentar.";
                            repaint();
                        }
                    }
                });
            }
        }).start();
    }

    private void drawBrand(Graphics g, int width, int height) {
        g.setColor(COLOR_GREEN);
        g.fillArc(width / 2 - 20, height / 4 - 20, 40, 40, 0, 360);
        g.setColor(COLOR_WHITE);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        g.drawString("W", width / 2, height / 4 + 7, Graphics.HCENTER | Graphics.TOP);
        g.setColor(COLOR_BLACK);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("WPJava", width / 2, height / 3, Graphics.HCENTER | Graphics.TOP);
    }

    private void drawButton(Graphics g, String label, int x, int y, int width,
            int height, int background, int foreground) {
        g.setColor(background);
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setColor(foreground);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString(label, x + width / 2, y + height / 2 - 5,
                Graphics.HCENTER | Graphics.TOP);
    }

    private void drawSoftkeys(Graphics g, int width, int height) {
        g.setColor(COLOR_LIGHT_GRAY);
        g.fillRect(0, height - 20, width, 20);
        g.setColor(COLOR_BLACK);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("Permitir", 5, height - 17, Graphics.LEFT | Graphics.TOP);
        g.drawString("Salir", width - 5, height - 17, Graphics.RIGHT | Graphics.TOP);
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxWidth) {
        String[] words = split(text, ' ');
        StringBuffer line = new StringBuffer();
        int lineY = y;
        int i;
        for (i = 0; i < words.length; i++) {
            String candidate = line.length() == 0 ? words[i] : line.toString() + " " + words[i];
            if (line.length() > 0 && g.getFont().stringWidth(candidate) > maxWidth) {
                g.drawString(line.toString(), x + maxWidth / 2, lineY,
                        Graphics.HCENTER | Graphics.TOP);
                line.setLength(0);
                line.append(words[i]);
                lineY += g.getFont().getHeight() + 2;
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x + maxWidth / 2, lineY,
                    Graphics.HCENTER | Graphics.TOP);
        }
    }

    private String[] split(String text, char delimiter) {
        Vector words = new Vector();
        int start = 0;
        int i;
        for (i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == delimiter) {
                if (i > start) words.addElement(text.substring(start, i));
                start = i + 1;
            }
        }
        String[] result = new String[words.size()];
        for (i = 0; i < result.length; i++) result[i] = (String) words.elementAt(i);
        return result;
    }

    private boolean hit(int x, int y, int left, int top, int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }
}
