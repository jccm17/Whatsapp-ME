package com.wpjava.storage;

import java.util.Vector;

import com.wpjava.model.Message;
import com.wpjava.service.ApiClient;

public class MessageStorage extends RecordStorage {

    private static final String STORE = "wp_messages";

    public void save(Message m) {
        if (m != null) {
            add(STORE, encode(m));
        }
    }

    public Vector getMessages(String chatId) {
        String[] rows = readAll(STORE);
        Vector messages = new Vector();
        int i;
        for (i = 0; i < rows.length; i++) {
            Message m = decode(rows[i]);
            if (m != null && same(m.chatId, chatId)) {
                messages.addElement(m);
            }
        }
        if (messages.size() == 0) {
            messages.addElement(sample(chatId, "Hola", true));
            messages.addElement(sample(chatId, "Hola amigo", false));
            messages.addElement(sample(chatId, "Como estas?", true));
        }
        return messages;
    }

    public int countUnread() {
        return 0;
    }

    public void clear() {
        clear(STORE);
    }

    private Message sample(String chatId, String body, boolean incoming) {
        Message m = new Message();
        m.id = String.valueOf(System.currentTimeMillis());
        m.chatId = chatId;
        m.body = body;
        m.incoming = incoming;
        m.status = "ok";
        m.timestamp = System.currentTimeMillis();
        return m;
    }

    private boolean same(String a, String b) {
        return a != null && b != null
                && ApiClient.normalizeRecipient(a).equals(ApiClient.normalizeRecipient(b));
    }

    private String encode(Message m) {
        return safe(m.id) + "|" + safe(m.chatId) + "|" + safe(m.body) + "|"
                + safe(m.type) + "|" + (m.incoming ? "1" : "0") + "|"
                + safe(m.status) + "|" + m.timestamp + "|"
                + safe(m.mediaUrl) + "|" + safe(m.mediaType);
    }

    private Message decode(String row) {
        try {
            String[] p = split(row, '|');
            Message m = new Message();
            m.id = value(p, 0);
            m.chatId = value(p, 1);
            m.body = value(p, 2);
            m.type = value(p, 3);
            m.incoming = "1".equals(value(p, 4));
            m.status = value(p, 5);
            m.timestamp = parseLong(value(p, 6));
            m.mediaUrl = value(p, 7);
            m.mediaType = value(p, 8);
            return m;
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
