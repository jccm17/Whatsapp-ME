package com.wpjava.service;

import java.util.Vector;

import com.wpjava.core.Config;
import com.wpjava.util.HttpUtil;
import com.wpjava.util.JsonParser;

public class ApiClient {

    public boolean ping() {
        return isConnected() || hasQR();
    }

    public String getStatus() {
        return HttpUtil.get(Config.API_URL + "/status");
    }

    public boolean isConnected() {
        String response = getStatus();
        return response != null && response.indexOf("connected") >= 0;
    }

    public boolean hasQR() {
        String response = getStatus();
        return response != null && response.indexOf("\"qr\"") >= 0;
    }

    public byte[] getQR() {
        return HttpUtil.getBytes(Config.API_URL + "/qr");
    }

    public boolean isLinked() {
        return isConnected();
    }

    public Vector getChats() {
        String history = HttpUtil.get(Config.API_URL + "/messaging-history");
        Vector chats = JsonParser.parseChatsFromMessages(history);
        if (chats.size() == 0) {
            chats = JsonParser.parseChats(HttpUtil.get(Config.API_URL + "/chats"));
        }
        return chats;
    }

    public Vector getMessages(String chatId) {
        Vector messages = JsonParser.parseMessages(
                HttpUtil.get(Config.API_URL + "/messaging-history?chatId="
                        + normalizeRecipient(chatId)),
                chatId);
        if (messages.size() == 0) {
            messages = JsonParser.parseMessages(
                    HttpUtil.get(Config.API_URL + "/messaging-history"),
                    chatId);
        }
        return messages;
    }

    public boolean sendMessage(String id, String text) {
        String body = "{\"to\":\"" + escape(normalizeRecipient(id)) + "\",\"message\":\""
                + escape(text) + "\"}";
        String response = HttpUtil.post(Config.API_URL + "/send", body);
        return response.indexOf("true") >= 0 || response.indexOf("ok") >= 0;
    }

    public static String normalizeRecipient(String value) {
        if (value == null) {
            return "";
        }
        String to = removeSpaces(value);
        if (to.indexOf('@') >= 0) {
            return to;
        }
        return to + "@s.whatsapp.net";
    }

    public boolean logout() {
        return false;
    }

    public void sync() {
        HttpUtil.get(Config.API_URL + "/messaging-history");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        int i;
        for (i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String removeSpaces(String value) {
        StringBuffer sb = new StringBuffer();
        int i;
        for (i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
