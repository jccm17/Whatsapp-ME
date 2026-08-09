package com.wpjava.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import com.wpjava.core.Router;
import com.wpjava.model.Chat;
import com.wpjava.model.Message;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.MessageStorage;

/** Screen for composing a message with a WhatsApp-inspired layout. */
public class NewMessageScreen extends Canvas implements CommandListener, Runnable {

    private static final int GREEN_DARK = 7;
    private static final int GREEN = 18;
    private static final int LIGHT_GREEN = 37;
    private static final int BACKGROUND = 239;
    private static final int TEXT = 35;

    private Command sendCommand = new Command("Enviar", Command.OK, 1);
    private Command emoticonCommand = new Command(":-)", Command.SCREEN, 2);
    private Command backCommand = new Command("Atras", Command.BACK, 3);
    private Command saveCommand = new Command("Listo", Command.OK, 1);
    private Command cancelEditCommand = new Command("Cancelar", Command.BACK, 2);
    private String recipient = "";
    private String message = "";
    private String pendingTo;
    private String pendingMessage;
    private int selectedField;
    private int editingField = -1;
    private TextBox editor;
    private Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    private Font labelFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private Font normalFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    public NewMessageScreen() {
        addCommand(sendCommand);
        addCommand(emoticonCommand);
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (d == editor) {
            finishEditing(c == saveCommand);
        } else if (c == sendCommand) {
            send();
        } else if (c == emoticonCommand) {
            Router.navigate(new EmoticonPickerScreen(this));
        } else if (c == backCommand) {
            Router.navigate(new ChatListScreen());
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == UP || action == DOWN) {
            selectedField = selectedField == 0 ? 1 : 0;
            repaint();
        } else if (action == FIRE) {
            editSelected();
        }
    }

    protected void pointerPressed(int x, int y) {
        if (x >= getWidth() - 86 && y >= getHeight() - 56) {
            send();
        } else if (y >= 72 && y < 126) {
            selectedField = 0;
            editSelected();
        } else if (y >= 137 && y < 215) {
            selectedField = 1;
            editSelected();
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(BACKGROUND, 235, 228);
        g.fillRect(0, 0, w, h);

        g.setColor(GREEN_DARK, 94, 84);
        g.fillRect(0, 0, w, 51);
        g.setColor(255, 255, 255);
        g.setFont(titleFont);
        g.drawString("Nuevo mensaje", 14, 11, Graphics.LEFT | Graphics.TOP);
        g.setFont(normalFont);
        g.drawString("Escribe a alguien", 14, 31, Graphics.LEFT | Graphics.TOP);

        g.setColor(255, 255, 255);
        g.fillRoundRect(8, 62, w - 16, 164, 8, 8);
        drawRecipient(g, w);
        drawMessage(g, w);

        g.setColor(105, 105, 105);
        g.setFont(normalFont);
        g.drawString("Selecciona un campo y pulsa el centro para editar", w / 2,
                h - 27, Graphics.HCENTER | Graphics.TOP);
        g.setColor(LIGHT_GREEN, 211, 102);
        g.fillRoundRect(w - 76, h - 48, 64, 25, 12, 12);
        g.setColor(255, 255, 255);
        g.setFont(labelFont);
        g.drawString("ENVIAR", w - 44, h - 42, Graphics.HCENTER | Graphics.TOP);
    }

    private void drawRecipient(Graphics g, int w) {
        int border = selectedField == 0 ? LIGHT_GREEN : 218;
        g.setColor(border, selectedField == 0 ? 211 : 218, selectedField == 0 ? 102 : 218);
        g.drawRoundRect(16, 73, w - 32, 45, 7, 7);
        g.setColor(GREEN, 140, 126);
        g.fillArc(25, 82, 27, 27, 0, 360);
        g.setColor(255, 255, 255);
        g.setFont(labelFont);
        g.drawString("+", 38, 87, Graphics.HCENTER | Graphics.TOP);
        g.setColor(105, 105, 105);
        g.setFont(normalFont);
        g.drawString("PARA", 61, 80, Graphics.LEFT | Graphics.TOP);
        g.setColor(TEXT, TEXT, TEXT);
        g.setFont(labelFont);
        g.drawString(shorten(recipient.length() == 0 ? "Numero o grupo" : recipient, w - 82),
                61, 96, Graphics.LEFT | Graphics.TOP);
    }

    private void drawMessage(Graphics g, int w) {
        int border = selectedField == 1 ? LIGHT_GREEN : 218;
        g.setColor(border, selectedField == 1 ? 211 : 218, selectedField == 1 ? 102 : 218);
        g.drawRoundRect(16, 137, w - 32, 76, 7, 7);
        g.setColor(105, 105, 105);
        g.setFont(normalFont);
        g.drawString("MENSAJE", 27, 146, Graphics.LEFT | Graphics.TOP);
        g.setColor(TEXT, TEXT, TEXT);
        g.setFont(normalFont);
        String visible = message.length() == 0 ? "Escribe un mensaje" : message;
        g.drawString(shorten(visible, w - 54), 27, 166, Graphics.LEFT | Graphics.TOP);
        if (message.length() == 0) {
            g.setColor(145, 145, 145);
            g.drawString("Puedes usar :-) para anadir emoticonos", 27, 187,
                    Graphics.LEFT | Graphics.TOP);
        }
    }

    private void editSelected() {
        editingField = selectedField;
        String title = editingField == 0 ? "Destinatario" : "Mensaje";
        String value = editingField == 0 ? recipient : message;
        int maxSize = editingField == 0 ? 80 : 500;
        editor = new TextBox(title, value, maxSize, TextField.ANY);
        editor.addCommand(saveCommand);
        editor.addCommand(cancelEditCommand);
        editor.setCommandListener(this);
        Router.getDisplay().setCurrent(editor);
    }

    private void finishEditing(boolean save) {
        if (save) {
            if (editingField == 0) {
                recipient = editor.getString();
            } else {
                message = editor.getString();
            }
        }
        editor = null;
        editingField = -1;
        Router.navigate(this);
        repaint();
    }

    private void send() {
        pendingTo = ApiClient.normalizeRecipient(clean(recipient));
        pendingMessage = message;
        if (pendingTo.length() == 0 || isBlank(pendingMessage)) {
            showError("Completa el destinatario y el mensaje.");
            return;
        }
        new Thread(this).start();
    }

    public void run() {
        ApiClient api = new ApiClient();
        boolean sent = api.sendMessage(pendingTo, pendingMessage);
        Message m = new Message();
        m.id = String.valueOf(System.currentTimeMillis());
        m.chatId = pendingTo;
        m.body = pendingMessage;
        m.incoming = false;
        m.status = sent ? "sent" : "local";
        m.timestamp = System.currentTimeMillis();
        new MessageStorage().save(m);
        if (sent) {
            Router.navigate(new ChatScreen(createChat(pendingTo)));
        } else {
            Alert alert = new Alert("Aviso", "No se confirmo el envio. Se guardo localmente.",
                    null, AlertType.WARNING);
            alert.setTimeout(2500);
            Router.getDisplay().setCurrent(alert, new ChatScreen(createChat(pendingTo)));
        }
    }

    public void appendEmoticon(String value) {
        if (message.length() > 0 && !endsWithSpace(message)) {
            message = message + " ";
        }
        message = message + value;
        selectedField = 1;
        repaint();
    }

    private Chat createChat(String to) {
        Chat chat = new Chat();
        chat.id = to;
        chat.name = displayName(to);
        chat.lastMessage = pendingMessage == null ? "" : pendingMessage;
        chat.timestamp = System.currentTimeMillis();
        return chat;
    }

    private String clean(String value) {
        StringBuffer sb = new StringBuffer();
        int i;
        for (i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t') sb.append(ch);
        }
        return sb.toString();
    }

    private boolean isBlank(String value) {
        if (value == null) return true;
        int i;
        for (i = 0; i < value.length(); i++) if (value.charAt(i) > ' ') return false;
        return true;
    }

    private String displayName(String to) {
        int at = to.indexOf('@');
        return at > 0 ? to.substring(0, at) : to;
    }

    private String shorten(String value, int maxWidth) {
        if (normalFont.stringWidth(value) <= maxWidth) return value;
        while (value.length() > 0 && normalFont.stringWidth(value + "...") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "...";
    }

    private boolean endsWithSpace(String value) {
        return value.length() == 0 || value.charAt(value.length() - 1) <= ' ';
    }

    private void showError(String text) {
        Alert alert = new Alert("Falta dato", text, null, AlertType.WARNING);
        alert.setTimeout(2000);
        Router.getDisplay().setCurrent(alert, this);
    }
}
