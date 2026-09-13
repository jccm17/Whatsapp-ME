package com.wpjava.util;

import java.util.Vector;

import com.wpjava.model.Chat;
import com.wpjava.model.Message;
import com.wpjava.service.ApiClient;
import com.wpjava.storage.Storage;

public class JsonParser {

    public static Vector parseChats(String json) {
        Vector chats = new Vector();
        if (json == null || json.length() == 0) {
            return chats;
        }
        Vector objects = objectsFromArray(json, "chats");
        if (objects.size() == 0) {
            objects = objectsFromArray(json, "messages");
        }
        int index;
        for (index = 0; index < objects.size(); index++) {
            String item = (String) objects.elementAt(index);
            Chat c = new Chat();
            c.id = normalizeField(valueOrEmpty(item, "id"));
            if (c.id.length() == 0) {
                c.id = normalizeField(valueOrEmpty(item, "chatId"));
            }
            if (c.id.length() == 0) {
                c.id = normalizeField(valueOrEmpty(item, "from"));
            }
            c.name = valueOrEmpty(item, "name");
            if (c.name.length() == 0) {
                c.name = displayName(c.id);
            }
            c.lastMessage = valueOrEmpty(item, "lastMessage");
            if (c.lastMessage.length() == 0) {
                c.lastMessage = valueOrEmpty(item, "last_message");
            }
            if (c.lastMessage.length() == 0) {
                c.lastMessage = valueOrEmpty(item, "text");
            }
            c.lastMessage = EmojiUtil.toDisplay(c.lastMessage);
            c.timestamp = getLong(item, "timestamp");
            c.unread = parseInt(valueOrEmpty(item, "unread"));
            c.avatar = fullUrl(valueOrEmpty(item, "avatarUrl"));
            c.archived = "true".equals(valueOrEmpty(item, "archived"));
            if (c.id.length() > 0) {
                chats.addElement(c);
            }
        }
        return chats;
    }

    public static Vector parseChatsFromMessages(String json) {
        Vector messages = parseMessages(json, null);
        Vector chats = new Vector();
        int i;
        for (i = 0; i < messages.size(); i++) {
            Message m = (Message) messages.elementAt(i);
            Chat c = findChat(chats, m.chatId);
            if (c == null) {
                c = new Chat();
                c.id = m.chatId;
                c.name = displayName(m.chatId);
                c.unread = 0;
                chats.addElement(c);
            }
            c.lastMessage = m.body;
            c.timestamp = m.timestamp;
            c.unread++;
        }
        return chats;
    }

    public static Vector parseMessages(String json, String chatId) {
        Vector messages = new Vector();
        if (json == null || json.length() == 0) {
            return messages;
        }
        Vector objects = objectsFromArray(json, "messages");
        if (objects.size() == 0) {
            objects = allObjects(json);
        }
        int index;
        for (index = 0; index < objects.size(); index++) {
            String item = (String) objects.elementAt(index);
            Message m = new Message();
            m.id = valueOrEmpty(item, "id");
            String from = normalizeField(valueOrEmpty(item, "from"));
            String to = normalizeField(valueOrEmpty(item, "to"));
            String sendTo = normalizeField(valueOrEmpty(item, "sendTo"));
            String itemChatId = normalizeField(valueOrEmpty(item, "chatId"));
            if (itemChatId.length() == 0) {
                itemChatId = sendTo.length() > 0 ? sendTo
                        : (from.length() == 0 || "me".equals(from) ? to : from);
            }
            if (chatId == null) {
                m.chatId = sendTo.length() > 0 ? sendTo : itemChatId;
            } else {
                m.chatId = ApiClient.normalizeRecipient(chatId);
            }
            m.body = valueOrEmpty(item, "body");
            if (m.body.length() == 0) {
                m.body = valueOrEmpty(item, "text");
            }
            m.body = EmojiUtil.toMessageDisplay(m.body);
            m.timestamp = getLong(item, "timestamp");
            m.mediaUrl = fullUrl(valueOrEmpty(item, "mediaUrl"));
            m.mediaType = valueOrEmpty(item, "mediaType");
            m.incoming = !"false".equals(valueOrEmpty(item, "incoming"));
            if (m.body.length() > 0
                    && (chatId == null
                    || sameChat(ApiClient.normalizeRecipient(chatId), itemChatId)
                    || sameChat(ApiClient.normalizeRecipient(chatId), from)
                    || sameChat(ApiClient.normalizeRecipient(chatId), to)
                    || sameChat(ApiClient.normalizeRecipient(chatId), sendTo))) {
                messages.addElement(m);
            }
        }
        return messages;
    }

    public static boolean linked(String json) {
        return getBoolean(json, "linked");
    }

    /** Returns a scalar JSON value, or null when the key is absent or not scalar. */
    public static String getString(String json, String name) {
        int valueStart = findValueStart(json, name);
        if (valueStart < 0) {
            return null;
        }
        if (json.charAt(valueStart) == '"') {
            int end = findStringEnd(json, valueStart);
            return end < 0 ? null : unescape(json.substring(valueStart + 1, end));
        }
        char first = json.charAt(valueStart);
        if (first == '{' || first == '[') {
            return null;
        }
        int end = valueStart;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if (ch == ',' || ch == '}' || ch == ']') {
                break;
            }
            end++;
        }
        return trim(json.substring(valueStart, end));
    }

    public static long getLong(String json, String name) {
        return parseLong(getString(json, name));
    }

    public static boolean getBoolean(String json, String name) {
        return "true".equalsIgnoreCase(getString(json, name));
    }

    /** Returns raw JSON elements from the named array, preserving nested values. */
    public static Vector getArray(String json, String name) {
        Vector elements = new Vector();
        int start = findValueStart(json, name);
        if (start < 0 || json.charAt(start) != '[') {
            return elements;
        }
        int end = findMatching(json, start, '[', ']');
        if (end < 0) {
            return elements;
        }
        splitElements(json.substring(start + 1, end), elements);
        return elements;
    }

    private static String valueOrEmpty(String json, String name) {
        String value = getString(json, name);
        return value == null ? "" : value;
    }

    private static int findValueStart(String json, String name) {
        if (json == null || name == null) {
            return -1;
        }
        int i = 0;
        while (i < json.length()) {
            if (json.charAt(i) != '"') {
                i++;
                continue;
            }
            int keyEnd = findStringEnd(json, i);
            if (keyEnd < 0) {
                return -1;
            }
            if (name.equals(unescape(json.substring(i + 1, keyEnd)))) {
                int colon = keyEnd + 1;
                while (colon < json.length() && json.charAt(colon) <= ' ') {
                    colon++;
                }
                if (colon < json.length() && json.charAt(colon) == ':') {
                    int valueStart = colon + 1;
                    while (valueStart < json.length() && json.charAt(valueStart) <= ' ') {
                        valueStart++;
                    }
                    return valueStart < json.length() ? valueStart : -1;
                }
            }
            i = keyEnd + 1;
        }
        return -1;
    }

    private static int findStringEnd(String text, int start) {
        int i;
        boolean escaped = false;
        for (i = start + 1; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return i;
            }
        }
        return -1;
    }

    private static void splitElements(String text, Vector elements) {
        int depth = 0;
        int start = 0;
        boolean inString = false;
        boolean escaped = false;
        int i;
        for (i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
            } else if (ch == '"') {
                inString = true;
            } else if (ch == '{' || ch == '[') {
                depth++;
            } else if (ch == '}' || ch == ']') {
                depth--;
            } else if (ch == ',' && depth == 0) {
                addElement(text, start, i, elements);
                start = i + 1;
            }
        }
        addElement(text, start, text.length(), elements);
    }

    private static void addElement(String text, int start, int end, Vector elements) {
        String element = trim(text.substring(start, end));
        if (element.length() > 0) {
            elements.addElement(element);
        }
    }

    private static String unescape(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuffer result = new StringBuffer(value.length());
        int i;
        for (i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != '\\' || i + 1 >= value.length()) {
                result.append(ch);
                continue;
            }
            char escaped = value.charAt(++i);
            if (escaped == 'b') result.append('\b');
            else if (escaped == 'f') result.append('\f');
            else if (escaped == 'n') result.append('\n');
            else if (escaped == 'r') result.append('\r');
            else if (escaped == 't') result.append('\t');
            else if (escaped == 'u' && i + 4 < value.length()) {
                int code = hex(value.substring(i + 1, i + 5));
                if (code >= 0) {
                    result.append((char) code);
                    i += 4;
                } else {
                    result.append('u');
                }
            } else {
                result.append(escaped);
            }
        }
        return result.toString();
    }

    private static Vector objectsFromArray(String json, String arrayName) {
        return getArray(json, arrayName);
    }

    private static Vector allObjects(String json) {
        Vector objects = new Vector();
        int i = 0;
        while (i < json.length()) {
            int start = json.indexOf('{', i);
            if (start < 0) {
                break;
            }
            int end = findMatching(json, start, '{', '}');
            if (end < 0) {
                break;
            }
            objects.addElement(json.substring(start, end + 1));
            i = end + 1;
        }
        return objects;
    }

    private static int findMatching(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int i;
        for (i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
            } else if (ch == '"') {
                inString = true;
            } else if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) <= ' ') {
            end--;
        }
        return value.substring(start, end);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int hex(String value) {
        try {
            return Integer.parseInt(value, 16);
        } catch (Exception e) {
            return -1;
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static Chat findChat(Vector chats, String id) {
        String normalized = ApiClient.normalizeRecipient(id);
        int i;
        for (i = 0; i < chats.size(); i++) {
            Chat c = (Chat) chats.elementAt(i);
            if (sameChat(c.id, normalized)) {
                return c;
            }
        }
        return null;
    }

    private static boolean sameChat(String a, String b) {
        return a != null && b != null
                && ApiClient.normalizeRecipient(a).equals(ApiClient.normalizeRecipient(b));
    }

    private static String displayName(String jid) {
        if (jid == null || jid.length() == 0) {
            return "Contacto";
        }
        int at = jid.indexOf('@');
        if (at > 0) {
            return jid.substring(0, at);
        }
        return jid;
    }

    private static String normalizeField(String value) {
        if (value == null || value.length() == 0 || "me".equals(value)) {
            return value == null ? "" : value;
        }
        return ApiClient.normalizeRecipient(value);
    }

    private static String fullUrl(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        if (value.indexOf("http://") == 0 || value.indexOf("https://") == 0) {
            return value;
        }
        if (value.charAt(0) == '/') {
            String server = Storage.getInstance().getServerUrl();
            while (server.endsWith("/")) {
                server = server.substring(0, server.length() - 1);
            }
            return server + value;
        }
        return value;
    }
}
