package com.wpjava.storage;

import java.util.Vector;

import com.wpjava.model.Chat;

public class ChatStorage extends RecordStorage {

    private static final String STORE = "wp_chats";

    public void saveChat(Chat c) {
        if (c != null) {
            add(STORE, encode(c));
        }
    }

    public Vector getChats() {
        String[] rows = readAll(STORE);
        Vector chats = new Vector();
        int i;
        for (i = 0; i < rows.length; i++) {
            Chat c = decode(rows[i]);
            if (c != null) {
                chats.addElement(c);
            }
        }
        if (chats.size() == 0) {
            chats.addElement(sample("1", "Juan", "Hola", 0));
            chats.addElement(sample("2", "Arek", "Hola como estas", 1));
        }
        return chats;
    }

    public void clear() {
        clear(STORE);
    }

    private Chat sample(String id, String name, String last, int unread) {
        Chat c = new Chat();
        c.id = id;
        c.name = name;
        c.lastMessage = last;
        c.timestamp = System.currentTimeMillis();
        c.unread = unread;
        return c;
    }

    private String encode(Chat c) {
        return safe(c.id) + "|" + safe(c.name) + "|" + safe(c.avatar) + "|"
                + safe(c.lastMessage) + "|" + c.timestamp + "|" + c.unread;
    }

    private Chat decode(String row) {
        try {
            String[] p = split(row, '|');
            Chat c = new Chat();
            c.id = value(p, 0);
            c.name = value(p, 1);
            c.avatar = value(p, 2);
            c.lastMessage = value(p, 3);
            c.timestamp = parseLong(value(p, 4));
            c.unread = parseInt(value(p, 5));
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('|', ' ');
    }

    private String value(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private String[] split(String value, char delimiter) {
        Vector parts = new Vector();
        int start = 0;
        int i;
        for (i = 0; i < value.length(); i++) {
            if (value.charAt(i) == delimiter) {
                parts.addElement(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.addElement(value.substring(start));
        String[] result = new String[parts.size()];
        for (i = 0; i < result.length; i++) {
            result[i] = (String) parts.elementAt(i);
        }
        return result;
    }
}
