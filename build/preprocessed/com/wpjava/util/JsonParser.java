package com.wpjava.util;

import java.util.Vector;

import com.wpjava.model.Chat;
import com.wpjava.model.Message;
import com.wpjava.core.Config;
import com.wpjava.service.ApiClient;

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
            c.id = normalizeField(field(item, "id"));
            if (c.id.length() == 0) {
                c.id = normalizeField(field(item, "chatId"));
            }
            if (c.id.length() == 0) {
                c.id = normalizeField(field(item, "from"));
            }
            c.name = field(item, "name");
            if (c.name.length() == 0) {
                c.name = displayName(c.id);
            }
            c.lastMessage = field(item, "lastMessage");
            if (c.lastMessage.length() == 0) {
                c.lastMessage = field(item, "last_message");
            }
            if (c.lastMessage.length() == 0) {
                c.lastMessage = field(item, "text");
            }
            c.lastMessage = EmojiUtil.toDisplay(c.lastMessage);
            c.timestamp = parseLong(field(item, "timestamp"));
            c.unread = parseInt(field(item, "unread"));
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
            m.id = field(item, "id");
            String from = normalizeField(field(item, "from"));
            String to = normalizeField(field(item, "to"));
            String sendTo = normalizeField(field(item, "sendTo"));
            String itemChatId = normalizeField(field(item, "chatId"));
            if (itemChatId.length() == 0) {
                itemChatId = sendTo.length() > 0 ? sendTo
                        : (from.length() == 0 || "me".equals(from) ? to : from);
            }
            if (chatId == null) {
                m.chatId = sendTo.length() > 0 ? sendTo : itemChatId;
            } else {
                m.chatId = ApiClient.normalizeRecipient(chatId);
            }
            m.body = field(item, "body");
            if (m.body.length() == 0) {
                m.body = field(item, "text");
            }
            m.body = EmojiUtil.toDisplay(m.body);
            m.timestamp = parseLong(field(item, "timestamp"));
            m.mediaUrl = fullUrl(field(item, "mediaUrl"));
            m.mediaType = field(item, "mediaType");
            m.incoming = !"false".equals(field(item, "incoming"));
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
        return json != null && json.indexOf("linked") >= 0
                && json.indexOf("true") >= 0;
    }

    private static String field(String json, String name) {
        String quoted = "\"" + name + "\"";
        int p = json.indexOf(quoted);
        if (p < 0) {
            return "";
        }
        int colon = json.indexOf(':', p + quoted.length());
        if (colon < 0) {
            return "";
        }
        int valueStart = colon + 1;
        while (valueStart < json.length() && json.charAt(valueStart) <= ' ') {
            valueStart++;
        }
        if (valueStart >= json.length()) {
            return "";
        }
        if (json.charAt(valueStart) == '"') {
            StringBuffer sb = new StringBuffer();
            int i;
            boolean escaped = false;
            for (i = valueStart + 1; i < json.length(); i++) {
                char ch = json.charAt(i);
                if (escaped) {
                    if (ch == 'u' && i + 4 < json.length()) {
                        int code = hex(json.substring(i + 1, i + 5));
                        if (code >= 0) {
                            sb.append((char) code);
                            i += 4;
                        }
                    } else if (ch == 'n') {
                        sb.append('\n');
                    } else if (ch == 't') {
                        sb.append('\t');
                    } else {
                        sb.append(ch);
                    }
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    return sb.toString();
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
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

    private static Vector objectsFromArray(String json, String arrayName) {
        String quoted = "\"" + arrayName + "\"";
        int p = json.indexOf(quoted);
        if (p < 0) {
            return new Vector();
        }
        int start = json.indexOf('[', p + quoted.length());
        if (start < 0) {
            return new Vector();
        }
        int end = findMatching(json, start, '[', ']');
        if (end < 0) {
            return new Vector();
        }
        return allObjects(json.substring(start + 1, end));
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
            return Config.API_URL + value;
        }
        return value;
    }
}
