package com.wpjava.ui;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import com.wpjava.WPJavaMidlet;
import com.wpjava.core.Router;
import com.wpjava.core.ScreenManager;
import com.wpjava.core.Theme;
import com.wpjava.model.Chat;
import com.wpjava.service.ApiClient;
import com.wpjava.service.RealtimeEvents;
import com.wpjava.storage.ChatStorage;
import com.wpjava.util.AvatarCache;
import com.wpjava.util.DateUtil;

public class ChatListScreen extends Canvas implements CommandListener, Runnable,
        ScreenManager.ScreenLifecycle, RealtimeEvents.Listener {

    private Command openCommand = new Command("Abrir", Command.OK, 1);
    private Command newCommand = new Command("Nuevo", Command.SCREEN, 2);
    private Command refreshCommand = new Command("Actualizar", Command.SCREEN, 3);
    private Command settingsCommand = new Command("Ajustes", Command.SCREEN, 4);
    private Command archivedCommand = new Command("Archivados", Command.SCREEN, 5);
    private Command exitCommand = new Command("Salir", Command.EXIT, 6);
    private Vector chats;
    private int archivedCount;
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
        addCommand(archivedCommand);
        addCommand(exitCommand);
        setCommandListener(this);
        loadLocal();
        new Thread(this).start();
    }

    private void loadLocal() {
        setVisibleChats(new ChatStorage().getChats());
        repaint();
    }

    private void setVisibleChats(Vector allChats) {
        chats = new Vector();
        archivedCount = 0;
        int i;
        for (i = 0; allChats != null && i < allChats.size(); i++) {
            Chat chat = (Chat) allChats.elementAt(i);
            if (chat.archived) {
                archivedCount++;
            } else {
                chats.addElement(chat);
            }
        }
        if (selectedIndex >= chats.size()) selectedIndex = chats.size() - 1;
        if (selectedIndex < 0) selectedIndex = 0;
        if (scroll < 0) scroll = 0;
    }

    public void onResume() {
        RealtimeEvents.addListener(this);
        loadLocal();
    }

    public void onPause() {
        RealtimeEvents.removeListener(this);
    }

    public void onRealtimeMessage(String chatId) { }

    public void onRealtimeChat(String chatId) {
        loadLocal();
    }

    public void run() {
        Vector remote = new ApiClient().getChats();
        if (remote != null && remote.size() > 0) {
            new ChatStorage().saveAll(remote);
            /* saveAll consolida JIDs equivalentes: mostrar la copia local
               evita pintar duplicados de la respuesta remota. */
            setVisibleChats(new ChatStorage().getChats());
            repaint();
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == openCommand) {
            openSelected();
        } else if (c == newCommand) {
            openNewChat();
        } else if (c == refreshCommand) {
            new Thread(this).start();
        } else if (c == settingsCommand) {
            Router.navigate(new SettingsScreen());
        } else if (c == archivedCommand) {
            Router.navigate(new ArchivedChatsScreen());
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
        if (y >= 5 && y <= 37 && x >= getWidth() - 92) {
            openNewChat();
            return;
        }
        if (y >= 5 && y <= 37 && x >= getWidth() - 166 && x < getWidth() - 92) {
            Router.navigate(new ArchivedChatsScreen());
            return;
        }
        int row = (y - 46 + scroll) / 54;
        if (chats != null && row >= 0 && row < chats.size()) {
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
        g.setColor(Theme.bgList());
        g.fillRect(0, 0, w, h);

        g.setColor(7, 94, 84);
        g.fillRect(0, 0, w, 42);
        g.setColor(255, 255, 255);
        g.setFont(titleFont);
        g.drawString("WhatsApp", 10, 10, Graphics.LEFT | Graphics.TOP);
        drawArchivedButton(g);
        drawNewButton(g, w);

        if (chats == null || chats.size() == 0) {
            g.setColor(90, 90, 90);
            g.drawString("Sin conversaciones", w / 2, h / 2 - 10,
                    Graphics.HCENTER | Graphics.TOP);
            g.drawString(archivedCount > 0 ? "Revisa Archivados" : "Usa Nuevo para escribir", w / 2, h / 2 + 10,
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

    private void drawNewButton(Graphics g, int width) {
        int x = width - 88;
        int y = 6;
        g.setColor(0xB2DFDB);
        g.fillRoundRect(x, y, 80, 30, 14, 14);
        g.setColor(0xFFFFFF);
        g.drawRoundRect(x, y, 80, 30, 14, 14);
        g.setColor(0x075E54);
        g.setFont(boldFont);
        g.fillArc(x + 8, y + 6, 19, 19, 0, 360);
        g.setColor(0xFFFFFF);
        g.drawArc(x + 11, y + 9, 13, 13, 0, 360);
        g.drawLine(x + 13, y + 20, x + 11, y + 23);
        g.drawLine(x + 16, y + 17, x + 20, y + 13);
        g.setColor(0x075E54);
        g.drawString("Nuevo", x + 54, y + 9, Graphics.HCENTER | Graphics.TOP);
    }

    private void drawArchivedButton(Graphics g) {
        int x = getWidth() - 166;
        int y = 7;
        g.setColor(0x075E54);
        g.fillRoundRect(x, y, 76, 27, 12, 12);
        g.setColor(0xB2DFDB);
        g.drawRoundRect(x, y, 76, 27, 12, 12);
        g.setColor(0xFFFFFF);
        g.setFont(normalFont);
        String label = archivedCount > 0 ? "Arch. " + archivedCount : "Arch.";
        g.drawString(label, x + 38, y + 8, Graphics.HCENTER | Graphics.TOP);
    }

    private void drawRow(Graphics g, Chat chat, int index, int y, int w) {
        if (y > getHeight() || y < -54) {
            return;
        }
        if (index == selectedIndex) {
            g.setColor(Theme.selected());
        } else {
            g.setColor(Theme.bgList());
        }
        g.fillRect(4, y, w - 8, 52);
        if (index == selectedIndex) {
            g.setColor(Theme.selectedBorder());
            g.fillRect(4, y, 4, 52);
            g.setColor(Theme.selectedBorder());
            g.drawRoundRect(4, y, w - 9, 51, 5, 5);
            g.setColor(255, 255, 255);
            g.setFont(boldFont);
            g.drawString(">", w - 12, y + 31, Graphics.RIGHT | Graphics.TOP);
        }

        g.setColor(chat.isGroup() ? 0x546E7A : 0x128C7E);
        g.fillArc(12, y + 9, 34, 34, 0, 360);
        Image avatar = chat.isGroup() ? null : AvatarCache.get(chat.avatar, 32, 32, this);
        if (avatar != null) {
            g.drawImage(avatar, 13, y + 10, Graphics.LEFT | Graphics.TOP);
        } else {
            g.setColor(255, 255, 255);
            g.setFont(boldFont);
            g.drawString(chat.isGroup() ? "G" : initial(chat.name), 29, y + 18,
                    Graphics.HCENTER | Graphics.TOP);
        }

        g.setColor(Theme.text());
        g.drawString(shorten(chat.name, w - 110, boldFont), 56, y + 7,
                Graphics.LEFT | Graphics.TOP);
        g.setFont(normalFont);
        g.setColor(Theme.textGray());
        String preview = chat.isGroup() ? "Grupo: " + chat.lastMessage : chat.lastMessage;
        g.drawString(shorten(preview, w - 92, normalFont), 56, y + 27,
                Graphics.LEFT | Graphics.TOP);

        g.setColor(Theme.textGray());
        g.drawString(DateUtil.time(chat.timestamp), w - 8, y + 7,
                Graphics.RIGHT | Graphics.TOP);
        if (chat.unread > 0) {
            g.setColor(37, 211, 102);
            g.fillArc(w - 28, y + 27, 18, 18, 0, 360);
            g.setColor(255, 255, 255);
            g.drawString(String.valueOf(chat.unread), w - 19, y + 29,
                    Graphics.HCENTER | Graphics.TOP);
        }
        g.setColor(Theme.sep());
        g.drawLine(56, y + 51, w - 8, y + 51);
    }

    private void openSelected() {
        if (chats != null && selectedIndex >= 0 && selectedIndex < chats.size()) {
            final Chat chat = (Chat) chats.elementAt(selectedIndex);
            chat.unread = 0;
            new ChatStorage().saveChat(chat);
            repaint();
            new Thread(new Runnable() {
                public void run() {
                    new ApiClient().markChatRead(chat.id);
                }
            }).start();
            Router.navigate(new ChatScreen(chat));
        }
    }

    private void openNewChat() {
        new NewChatScreen().show();
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
