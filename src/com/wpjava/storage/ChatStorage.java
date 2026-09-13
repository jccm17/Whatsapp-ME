package com.wpjava.storage;

import java.util.Vector;

import com.wpjava.model.Chat;
import com.wpjava.service.ApiClient;

public class ChatStorage extends RecordStorage {

    private static final String STORE = "wp_chats";

    public void saveChat(Chat c) {
        if (c == null || c.id == null || c.id.length() == 0) return;
        c.id = canonicalId(c.id);
        Vector chats = getChats();
        Chat current = findById(chats, c.id);
        if (current == null) {
            chats.addElement(c);
        } else {
            merge(current, c);
        }
        saveAll(chats);
    }

    public Vector getChats() {
        String[] rows = readAll(STORE);
        Vector chats = new Vector();
        int i;
        for (i = 0; i < rows.length; i++) {
            Chat c = decode(rows[i]);
            if (c != null) {
                c.id = canonicalId(c.id);
                if (c.id.length() > 0) {
                    Chat current = findById(chats, c.id);
                    if (current == null) {
                        chats.addElement(c);
                    } else {
                        merge(current, c);
                    }
                }
            }
        }
        return chats;
    }

    public void saveAll(Vector chats) {
        Vector unique = new Vector();
        int i;
        for (i = 0; chats != null && i < chats.size(); i++) {
            Chat chat = (Chat) chats.elementAt(i);
            if (chat == null || chat.id == null || chat.id.length() == 0) {
                continue;
            }
            chat.id = canonicalId(chat.id);
            Chat current = findById(unique, chat.id);
            if (current == null) {
                unique.addElement(chat);
            } else {
                merge(current, chat);
            }
        }
        String[] rows = new String[unique.size()];
        for (i = 0; i < unique.size(); i++) {
            rows[i] = encode((Chat) unique.elementAt(i));
        }
        replaceAll(STORE, rows);
    }

    public void clear() {
        clear(STORE);
    }

    private boolean sameId(String a, String b) {
        return a != null && a.length() > 0 && a.equals(b);
    }

    private Chat findById(Vector chats, String id) {
        int i;
        for (i = 0; i < chats.size(); i++) {
            Chat current = (Chat) chats.elementAt(i);
            if (sameId(current.id, id)) {
                return current;
            }
        }
        return null;
    }

    private String canonicalId(String id) {
        return ApiClient.normalizeRecipient(id);
    }

    private void merge(Chat current, Chat incoming) {
        boolean newer = incoming.timestamp >= current.timestamp;
        if (hasName(incoming) && (!hasName(current) || newer)) {
            current.name = incoming.name;
        }
        if (incoming.avatar != null && incoming.avatar.length() > 0) {
            current.avatar = incoming.avatar;
        }
        if (newer) {
            current.lastMessage = incoming.lastMessage;
            current.timestamp = incoming.timestamp;
            current.unread = incoming.unread;
            current.archived = incoming.archived;
        }
    }

    private boolean hasName(Chat chat) {
        if (chat.name == null || chat.name.length() == 0) {
            return false;
        }
        String id = chat.id == null ? "" : chat.id;
        int at = id.indexOf('@');
        if (at > 0) {
            id = id.substring(0, at);
        }
        return !chat.name.equals(id);
    }

    private String encode(Chat c) {
        return safe(c.id) + "|" + safe(c.name) + "|" + safe(c.avatar) + "|"
                + safe(c.lastMessage) + "|" + c.timestamp + "|" + c.unread + "|"
                + (c.archived ? "1" : "0");
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
            c.archived = "1".equals(value(p, 6));
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
