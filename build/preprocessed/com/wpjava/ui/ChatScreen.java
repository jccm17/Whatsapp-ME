package com.wpjava.ui;

import java.util.Vector;

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
import com.wpjava.util.DateUtil;
import com.wpjava.util.EmojiUtil;

public class ChatScreen extends Canvas implements CommandListener, Runnable {

    private static final int HEADER_HEIGHT = 44;
    private static final int INPUT_HEIGHT = 36;
    private static final int MESSAGE_TOP = HEADER_HEIGHT + 12;
    private static final int MESSAGE_BOTTOM_GAP = 8;
    private static final int BG_R = 236;
    private static final int BG_G = 229;
    private static final int BG_B = 221;

    private Command composeCommand = new Command("Enviar", Command.OK, 1);
    private Command refreshCommand = new Command("Actualizar", Command.SCREEN, 2);
    private Command backCommand = new Command("Atras", Command.BACK, 3);
    private Command sendTextCommand = new Command("Enviar", Command.OK, 1);
    private Command cancelTextCommand = new Command("Cancelar", Command.BACK, 2);

    private Chat chat;
    private MessageStorage storage;
    private Vector messages;
    private String pendingSend;
    private TextBox editor;
    private int scroll;

    private Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    private Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private Font normalFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private Font smallFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    public ChatScreen(Chat chat) {
        this.chat = chat;
        storage = new MessageStorage();
        messages = storage.getMessages(chat.id);
        addCommand(composeCommand);
        addCommand(refreshCommand);
        addCommand(backCommand);
        setCommandListener(this);
        scrollToBottom();
        new Thread(this).start();
    }

    public void commandAction(Command c, Displayable d) {
        if (d == editor) {
            if (c == sendTextCommand) {
                String text = editor.getString();
                editor = null;
                Router.navigate(this);
                queueSend(text);
            } else {
                editor = null;
                Router.navigate(this);
            }
        } else if (c == composeCommand) {
            openComposer();
        } else if (c == refreshCommand) {
            new Thread(this).start();
        } else if (c == backCommand) {
            Router.navigate(new ChatListScreen());
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == UP) {
            scroll -= 24;
            if (scroll < 0) {
                scroll = 0;
            }
            repaint();
        } else if (action == DOWN) {
            scroll += 24;
            limitScroll();
            repaint();
        } else if (action == FIRE) {
            openComposer();
        }
    }

    protected void pointerPressed(int x, int y) {
        if (y >= getHeight() - INPUT_HEIGHT) {
            openComposer();
        }
    }

    public void run() {
        if (pendingSend != null) {
            sendPending();
            return;
        }

        Vector remote = new ApiClient().getMessages(chat.id);
        if (remote != null && remote.size() > 0) {
            messages = remote;
            scrollToBottom();
            repaint();
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        drawBackground(g, w, h);
        drawHeader(g, w);
        drawMessages(g, w, h);
        drawHeader(g, w);
        drawInputBar(g, w, h);
    }

    private void drawBackground(Graphics g, int w, int h) {
        g.setColor(BG_R, BG_G, BG_B);
        g.fillRect(0, 0, w, h);

        g.setColor(226, 219, 211);
        int x;
        int y;
        for (y = HEADER_HEIGHT + 8; y < h - INPUT_HEIGHT; y += 24) {
            for (x = 8; x < w; x += 36) {
                g.drawArc(x, y, 8, 8, 0, 360);
            }
        }
    }

    private void drawHeader(Graphics g, int w) {
        g.setColor(7, 94, 84);
        g.fillRect(0, 0, w, HEADER_HEIGHT);

        g.setColor(18, 140, 126);
        g.fillArc(7, 7, 30, 30, 0, 360);
        g.setColor(255, 255, 255);
        g.setFont(boldFont);
        g.drawString(initial(chat.name), 22, 14, Graphics.HCENTER | Graphics.TOP);

        g.setFont(titleFont);
        g.drawString(shorten(chat.name, w - 78, titleFont), 45, 6,
                Graphics.LEFT | Graphics.TOP);
        g.setFont(smallFont);
        g.drawString(shorten(chat.id, w - 78, smallFont), 45, 25,
                Graphics.LEFT | Graphics.TOP);
    }

    private void drawMessages(Graphics g, int w, int h) {
        int y = MESSAGE_TOP - scroll;
        int i;
        g.setFont(normalFont);
        for (i = 0; i < messages.size(); i++) {
            Message m = (Message) messages.elementAt(i);
            int height = bubbleHeight(m, w);
            if (y + height >= MESSAGE_TOP && y <= h - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP) {
                drawBubble(g, m, y, height, w);
            }
            y += height + 6;
        }
    }

    private void drawBubble(Graphics g, Message m, int y, int height, int w) {
        boolean mine = !m.incoming;
        int maxWidth = w - 58;
        String body = EmojiUtil.toDisplay(m.body);
        int bubbleWidth = Math.min(maxWidth, Math.max(64,
                textWidth(body, maxWidth - 16) + 18));
        if (hasMedia(m)) {
            bubbleWidth = Math.max(bubbleWidth, 112);
        }

        int x = mine ? w - bubbleWidth - 8 : 8;
        if (mine) {
            g.setColor(220, 248, 198);
        } else {
            g.setColor(255, 255, 255);
        }
        g.fillRoundRect(x, y, bubbleWidth, height, 8, 8);
        g.setColor(mine ? 196 : 226, mine ? 226 : 226, mine ? 182 : 226);
        g.drawRoundRect(x, y, bubbleWidth, height, 8, 8);

        int textY = y + 7;
        if (hasMedia(m)) {
            g.setColor(238, 238, 238);
            g.fillRoundRect(x + 7, textY, bubbleWidth - 14, 42, 6, 6);
            g.setColor(18, 140, 126);
            g.setFont(boldFont);
            g.drawString("Imagen", x + bubbleWidth / 2, textY + 12,
                    Graphics.HCENTER | Graphics.TOP);
            textY += 48;
        }

        g.setFont(normalFont);
        g.setColor(25, 25, 25);
        drawWrapped(g, body, x + 8, textY, bubbleWidth - 16);

        g.setFont(smallFont);
        g.setColor(105, 105, 105);
        String meta = DateUtil.shortDate(m.timestamp);
        if (!m.incoming && m.status != null && m.status.length() > 0) {
            meta = meta + " " + statusMark(m.status);
        }
        g.drawString(meta, x + bubbleWidth - 7, y + height - 15,
                Graphics.RIGHT | Graphics.TOP);
    }

    private void drawInputBar(Graphics g, int w, int h) {
        int y = h - INPUT_HEIGHT;
        g.setColor(245, 245, 245);
        g.fillRect(0, y, w, INPUT_HEIGHT);

        g.setColor(255, 255, 255);
        g.fillRoundRect(8, y + 6, w - 64, 24, 12, 12);
        g.setColor(140, 140, 140);
        g.setFont(normalFont);
        g.drawString("Escribir mensaje", 20, y + 11, Graphics.LEFT | Graphics.TOP);

        g.setColor(37, 211, 102);
        g.fillArc(w - 46, y + 5, 26, 26, 0, 360);
        g.setColor(255, 255, 255);
        g.setFont(boldFont);
        g.drawString(">", w - 33, y + 10, Graphics.HCENTER | Graphics.TOP);
    }

    private void openComposer() {
        editor = new TextBox("Mensaje", "", 500, TextField.ANY);
        editor.addCommand(sendTextCommand);
        editor.addCommand(cancelTextCommand);
        editor.setCommandListener(this);
        Router.getDisplay().setCurrent(editor);
    }

    private void queueSend(String text) {
        if (isBlank(text)) {
            return;
        }
        pendingSend = text;
        Message m = new Message();
        m.id = String.valueOf(System.currentTimeMillis());
        m.chatId = chat.id;
        m.body = text;
        m.incoming = false;
        m.status = "sending";
        m.timestamp = System.currentTimeMillis();
        messages.addElement(m);
        scrollToBottom();
        repaint();
        new Thread(this).start();
    }

    private void sendPending() {
        String text = pendingSend;
        pendingSend = null;
        boolean sent = new ApiClient().sendMessage(chat.id, text);
        Message m = findLastLocal(text);
        if (sent) {
            if (m != null) {
                m.status = "sent";
                storage.save(m);
            }
            Vector remote = new ApiClient().getMessages(chat.id);
            if (remote != null && remote.size() > 0) {
                messages = remote;
            }
            scrollToBottom();
            repaint();
        } else {
            if (m != null) {
                m.status = "failed";
            }
            repaint();
            Alert alert = new Alert("No enviado",
                    "El backend rechazo el envio. Revisa conexion o JID del chat.",
                    null, AlertType.ERROR);
            alert.setTimeout(3000);
            Router.getDisplay().setCurrent(alert, this);
        }
    }

    private Message findLastLocal(String text) {
        int i;
        for (i = messages.size() - 1; i >= 0; i--) {
            Message m = (Message) messages.elementAt(i);
            if (!m.incoming && text.equals(m.body)) {
                return m;
            }
        }
        return null;
    }

    private int bubbleHeight(Message m, int w) {
        int maxWidth = w - 74;
        int lines = countLines(EmojiUtil.toDisplay(m.body), maxWidth);
        int height = 16 + lines * normalFont.getHeight() + 14;
        if (hasMedia(m)) {
            height += 48;
        }
        return Math.max(30, height);
    }

    private int contentHeight(int w) {
        int total = 0;
        int i;
        for (i = 0; i < messages.size(); i++) {
            total += bubbleHeight((Message) messages.elementAt(i), w) + 6;
        }
        return total;
    }

    private void scrollToBottom() {
        scroll = contentHeight(getWidth())
                - (getHeight() - MESSAGE_TOP - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP);
        if (scroll < 0) {
            scroll = 0;
        }
    }

    private void limitScroll() {
        int max = contentHeight(getWidth())
                - (getHeight() - MESSAGE_TOP - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP);
        if (max < 0) {
            max = 0;
        }
        if (scroll > max) {
            scroll = max;
        }
    }

    private int countLines(String text, int maxWidth) {
        if (text == null || text.length() == 0) {
            return 1;
        }
        int lines = 1;
        int width = 0;
        int i;
        for (i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int cw = normalFont.charWidth(ch);
            if (ch == '\n' || width + cw > maxWidth) {
                lines++;
                width = 0;
            }
            if (ch != '\n') {
                width += cw;
            }
        }
        return lines;
    }

    private int textWidth(String text, int maxWidth) {
        if (text == null || text.length() == 0) {
            return normalFont.stringWidth("Mensaje");
        }
        int max = 0;
        int width = 0;
        int i;
        for (i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int cw = normalFont.charWidth(ch);
            if (ch == '\n' || width + cw > maxWidth) {
                if (width > max) {
                    max = width;
                }
                width = 0;
            }
            if (ch != '\n') {
                width += cw;
            }
        }
        return width > max ? width : max;
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxWidth) {
        if (text == null || text.length() == 0) {
            text = "";
        }
        StringBuffer line = new StringBuffer();
        int width = 0;
        int lineY = y;
        int i;
        for (i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int cw = normalFont.charWidth(ch);
            if (ch == '\n' || width + cw > maxWidth) {
                g.drawString(line.toString(), x, lineY, Graphics.LEFT | Graphics.TOP);
                line.setLength(0);
                width = 0;
                lineY += normalFont.getHeight();
            }
            if (ch != '\n') {
                line.append(ch);
                width += cw;
            }
        }
        g.drawString(line.toString(), x, lineY, Graphics.LEFT | Graphics.TOP);
    }

    private boolean hasMedia(Message m) {
        return m.mediaUrl != null && m.mediaUrl.length() > 0;
    }

    private String statusMark(String status) {
        if ("sending".equals(status)) {
            return "...";
        }
        if ("failed".equals(status)) {
            return "!";
        }
        return "ok";
    }

    private boolean isBlank(String text) {
        if (text == null) {
            return true;
        }
        int i;
        for (i = 0; i < text.length(); i++) {
            if (text.charAt(i) > ' ') {
                return false;
            }
        }
        return true;
    }

    private String initial(String value) {
        if (value == null || value.length() == 0) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }

    private String shorten(String value, int maxWidth, Font font) {
        if (value == null) {
            value = "";
        }
        if (font.stringWidth(value) <= maxWidth) {
            return value;
        }
        while (value.length() > 0 && font.stringWidth(value + "...") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "...";
    }
}
