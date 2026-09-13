package com.wpjava.ui;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import com.wpjava.core.Router;
import com.wpjava.model.Chat;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.ChatStorage;

/** Separate list for chats archived from WhatsApp. */
public class ArchivedChatsScreen extends List implements CommandListener, Runnable {

    private final Command backCommand = new Command("Atras", Command.BACK, 1);
    private final Command refreshCommand = new Command("Actualizar", Command.SCREEN, 2);
    private final Vector archived = new Vector();

    public ArchivedChatsScreen() {
        super("Archivados", List.IMPLICIT);
        append("Cargando chats archivados...", null);
        addCommand(backCommand);
        addCommand(refreshCommand);
        setCommandListener(this);
        new Thread(this).start();
    }

    public void run() {
        Vector all = new ApiClient().getChats();
        if (all != null && all.size() > 0) {
            new ChatStorage().saveAll(all);
        } else {
            all = new ChatStorage().getChats();
        }
        synchronized (archived) {
            archived.removeAllElements();
            int i;
            for (i = 0; all != null && i < all.size(); i++) {
                Chat chat = (Chat) all.elementAt(i);
                if (chat.archived) archived.addElement(chat);
            }
        }
        Router.getDisplay().callSerially(new Runnable() {
            public void run() {
                deleteAll();
                synchronized (archived) {
                    if (archived.size() == 0) {
                        append("No hay chats archivados", null);
                    } else {
                        int i;
                        for (i = 0; i < archived.size(); i++) {
                            Chat chat = (Chat) archived.elementAt(i);
                            append(chat.name + "\n" + chat.lastMessage, null);
                        }
                    }
                }
            }
        });
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == backCommand) {
            Router.back();
        } else if (command == refreshCommand) {
            new Thread(this).start();
        } else {
            int selected = getSelectedIndex();
            synchronized (archived) {
                if (selected >= 0 && selected < archived.size()) {
                    Router.navigate(new ChatScreen((Chat) archived.elementAt(selected)));
                }
            }
        }
    }
}
