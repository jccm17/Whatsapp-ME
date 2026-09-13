package com.wpjava.storage;

import java.util.Vector;

import com.wpjava.model.Message;
import com.wpjava.service.ApiClient;

public class MessageStorage extends RecordStorage {

    private static final String STORE = "wp_messages";

    public void save(Message m) {
        if (m == null) return;
        Vector messages = getAll();
        int i;
        for (i = messages.size() - 1; i >= 0; i--) {
            Message current = (Message) messages.elementAt(i);
            if (sameId(current.id, m.id)) messages.removeElementAt(i);
        }
        messages.addElement(m);
        saveAll(messages);
    }

    public Vector getMessages(String chatId) {
        Vector all = getAll();
        Vector messages = new Vector();
        int i;
        for (i = 0; i < all.size(); i++) {
            Message m = (Message) all.elementAt(i);
            if (same(m.chatId, chatId)) {
                messages.addElement(m);
            }
        }
        return messages;
    }

    public void saveAll(Vector messages) {
        int size = messages == null ? 0 : messages.size();
        int start = size > 300 ? size - 300 : 0;
        String[] rows = new String[size - start];
        int i;
        for (i = start; i < size; i++) rows[i - start] = encode((Message) messages.elementAt(i));
        replaceAll(STORE, rows);
    }

    public int countUnread() {
        return 0;
    }

    public void updateStatus(String messageId, String status) {
        if (messageId == null || messageId.length() == 0) return;
        Vector messages = getAll();
        boolean changed = false;
        int i;
        for (i = 0; i < messages.size(); i++) {
            Message message = (Message) messages.elementAt(i);
            if (messageId.equals(message.id)) {
                message.status = status == null ? "" : status;
                changed = true;
            }
        }
        if (changed) saveAll(messages);
    }

    public void clear() {
        clear(STORE);
    }

    private Vector getAll() {
        String[] rows = readAll(STORE);
        Vector messages = new Vector();
        int i;
        for (i = 0; i < rows.length; i++) {
            Message m = decode(rows[i]);
            if (m != null) messages.addElement(m);
        }
        return messages;
    }

    private boolean same(String a, String b) {
        return a != null && b != null
                && ApiClient.normalizeRecipient(a).equals(ApiClient.normalizeRecipient(b));
    }

    private boolean sameId(String a, String b) {
        return a != null && a.length() > 0 && a.equals(b);
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
