package com.wpjava.ui;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.Router;
import com.wpjava.model.Chat;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.ChatStorage;
import com.wpjava.util.DateUtil;

public class ChatListScreen extends Canvas implements CommandListener, Runnable {

    private Command openCommand = new Command("Abrir", Command.OK, 1);
    private Command newCommand = new Command("Nuevo", Command.SCREEN, 2);
    private Command refreshCommand = new Command("Actualizar", Command.SCREEN, 3);
    private Command settingsCommand = new Command("Ajustes", Command.SCREEN, 4);
    private Command exitCommand = new Command("Salir", Command.EXIT, 5);
    private Vector chats;
    private int selectedIndex;
    private int scroll;
    private Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    private Font normalFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

    public ChatListScreen() {
        addCommand(openCommand);
        addCommand(newCommand);
        addCommand(refreshCommand);
        addCommand(settingsCommand);
        addCommand(exitCommand);
        setCommandListener(this);
        loadLocal();
        new Thread(this).start();
    }

    private void loadLocal() {
        chats = new ChatStorage().getChats();
        repaint();
    }

    public void run() {
        Vector remote = new ApiClient().getChats();
        if (remote != null && remote.size() > 0) {
            chats = remote;
            if (selectedIndex >= chats.size()) {
                selectedIndex = chats.size() - 1;
            }
            repaint();
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == openCommand) {
            openSelected();
        } else if (c == newCommand) {
            Router.navigate(new NewMessageScreen());
        } else if (c == refreshCommand) {
            new Thread(this).start();
        } else if (c == settingsCommand) {
            Router.navigate(new SettingsScreen());
        } else if (c == exitCommand) {
            WPJavaMidlet.getInstance().exit();
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == UP) {
            move(-1);
        } else if (action == DOWN) {
            move(1);
        } else if (action == FIRE) {
            openSelected();
        }
    }

    protected void pointerPressed(int x, int y) {
        int row = (y - 46 + scroll) / 54;
        if (row >= 0 && row < chats.size()) {
            if (selectedIndex == row) {
                openSelected();
            } else {
                selectedIndex = row;
                ensureVisible();
                repaint();
            }
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setFont(normalFont);
        g.setColor(236, 229, 221);
        g.fillRect(0, 0, w, h);

        g.setColor(7, 94, 84);
        g.fillRect(0, 0, w, 42);
        g.setColor(255, 255, 255);
        g.setFont(titleFont);
        g.drawString("WPJava", 10, 10, Graphics.LEFT | Graphics.TOP);
        g.setFont(normalFont);
        g.drawString("Nuevo", w - 8, 13, Graphics.RIGHT | Graphics.TOP);

        if (chats == null || chats.size() == 0) {
            g.setColor(90, 90, 90);
            g.drawString("Sin conversaciones", w / 2, h / 2 - 10,
                    Graphics.HCENTER | Graphics.TOP);
            g.drawString("Usa Nuevo para escribir", w / 2, h / 2 + 10,
                    Graphics.HCENTER | Graphics.TOP);
            return;
        }

        int y = 46 - scroll;
        int i;
        for (i = 0; i < chats.size(); i++) {
            drawRow(g, (Chat) chats.elementAt(i), i, y, w);
            y += 54;
        }
    }

    private void drawRow(Graphics g, Chat chat, int index, int y, int w) {
        if (y > getHeight() || y < -54) {
            return;
        }
        if (index == selectedIndex) {
            g.setColor(220, 248, 198);
        } else {
            g.setColor(255, 255, 255);
        }
        g.fillRect(4, y, w - 8, 52);

        g.setColor(18, 140, 126);
        g.fillArc(12, y + 9, 34, 34, 0, 360);
        g.setColor(255, 255, 255);
        g.setFont(boldFont);
        g.drawString(initial(chat.name), 29, y + 18, Graphics.HCENTER | Graphics.TOP);

        g.setColor(20, 20, 20);
        g.drawString(shorten(chat.name, w - 110, boldFont), 56, y + 7,
                Graphics.LEFT | Graphics.TOP);
        g.setFont(normalFont);
        g.setColor(90, 90, 90);
        g.drawString(shorten(chat.lastMessage, w - 92, normalFont), 56, y + 27,
                Graphics.LEFT | Graphics.TOP);

        g.setColor(110, 110, 110);
        g.drawString(DateUtil.shortDate(chat.timestamp), w - 8, y + 7,
                Graphics.RIGHT | Graphics.TOP);
        if (chat.unread > 0) {
            g.setColor(37, 211, 102);
            g.fillArc(w - 28, y + 27, 18, 18, 0, 360);
            g.setColor(255, 255, 255);
            g.drawString(String.valueOf(chat.unread), w - 19, y + 29,
                    Graphics.HCENTER | Graphics.TOP);
        }
        g.setColor(225, 225, 225);
        g.drawLine(56, y + 51, w - 8, y + 51);
    }

    private void openSelected() {
        if (chats != null && selectedIndex >= 0 && selectedIndex < chats.size()) {
            Router.navigate(new ChatScreen((Chat) chats.elementAt(selectedIndex)));
        }
    }

    private void move(int delta) {
        if (chats == null || chats.size() == 0) {
            return;
        }
        selectedIndex += delta;
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        if (selectedIndex >= chats.size()) {
            selectedIndex = chats.size() - 1;
        }
        ensureVisible();
        repaint();
    }

    private void ensureVisible() {
        int top = selectedIndex * 54;
        int bottom = top + 54;
        int area = getHeight() - 46;
        if (top < scroll) {
            scroll = top;
        } else if (bottom > scroll + area) {
            scroll = bottom - area;
        }
        if (scroll < 0) {
            scroll = 0;
        }
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
        String dots = "...";
        while (value.length() > 0 && font.stringWidth(value + dots) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + dots;
    }
}
