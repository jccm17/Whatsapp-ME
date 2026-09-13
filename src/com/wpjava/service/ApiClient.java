package com.wpjava.service;

import java.util.Vector;
import java.io.InputStream;

import com.wpjava.core.Config;
import com.wpjava.util.HttpUtil;
import com.wpjava.util.JsonParser;
import com.wpjava.storage.Storage;

public class ApiClient {

    public boolean ping() {
        return isConnected() || hasQR();
    }

    public String getStatus() {
        return HttpUtil.get(baseUrl() + "/status");
    }

    public boolean isConnected() {
        String response = getStatus();
        return "connected".equals(JsonParser.getString(response, "status"))
                || JsonParser.getBoolean(response, "connected");
    }

    public boolean hasQR() {
        return "qr".equals(JsonParser.getString(getStatus(), "status"));
    }

    public byte[] getQR() {
        return HttpUtil.getBytes(baseUrl() + "/qr");
    }

    public boolean isLinked() {
        return isConnected();
    }

    public Vector getChats() {
        Vector chats = JsonParser.parseChats(HttpUtil.get(baseUrl() + "/chats"));
        if (chats.size() == 0) {
            chats = JsonParser.parseChatsFromMessages(
                    HttpUtil.get(baseUrl() + "/messaging-history"));
        }
        return chats;
    }

    public boolean markChatRead(String id) {
        String response = HttpUtil.post(baseUrl() + "/chats/"
                + normalizeRecipient(id) + "/read", "{}");
        return response.indexOf("true") >= 0 || response.indexOf("ok") >= 0;
    }

    public Vector getMessages(String chatId) {
        Vector messages = JsonParser.parseMessages(
                HttpUtil.get(baseUrl() + "/messaging-history?chatId="
                        + normalizeRecipient(chatId)),
                chatId);
        if (messages.size() == 0) {
            messages = JsonParser.parseMessages(
                    HttpUtil.get(baseUrl() + "/messaging-history"),
                    chatId);
        }
        return messages;
    }

    public boolean sendMessage(String id, String text) {
        String body = "{\"to\":\"" + escape(normalizeRecipient(id)) + "\",\"message\":\""
                + escape(text) + "\"}";
        String response = HttpUtil.post(baseUrl() + "/send", body);
        return response.indexOf("true") >= 0 || response.indexOf("ok") >= 0;
    }

    public boolean sendAudio(String id, InputStream audio, long length) {
        return sendAudio(id, audio, length, "audio/amr");
    }

    public boolean sendAudio(String id, InputStream audio, long length, String mimeType) {
        String url = baseUrl() + "/send-audio?to=" + normalizeRecipient(id);
        if (mimeType == null || mimeType.length() == 0) mimeType = "audio/amr";
        String response = HttpUtil.postStream(url, mimeType, audio, length);
        return response.indexOf("true") >= 0 || response.indexOf("ok") >= 0;
    }

    public boolean sendImage(String id, InputStream image, long length) {
        String url = baseUrl() + "/send-image?to=" + normalizeRecipient(id);
        String response = HttpUtil.postStream(url, "image/jpeg", image, length);
        return response.indexOf("true") >= 0 || response.indexOf("ok") >= 0;
    }

    public static String normalizeRecipient(String value) {
        if (value == null) {
            return "";
        }
        String to = removeSpaces(value);
        if (to.indexOf('@') >= 0) {
            /* Baileys puede incluir el identificador de dispositivo en JIDs
               individuales (numero:dispositivo@s.whatsapp.net). El chat se
               identifica por el numero, no por el dispositivo. */
            int at = to.indexOf('@');
            int colon = to.indexOf(':');
            if (colon > 0 && colon < at && to.endsWith("@s.whatsapp.net")) {
                to = to.substring(0, colon) + to.substring(at);
            }
            return to;
        }
        to = digitsOnly(to);
        return to.length() == 0 ? "" : to + "@s.whatsapp.net";
    }

    public boolean logout() {
        return false;
    }

    public void sync() {
        HttpUtil.get(baseUrl() + "/messaging-history");
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

    private static String digitsOnly(String value) {
        StringBuffer sb = new StringBuffer();
        int i;
        for (i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String baseUrl() {
        return Storage.getInstance().getServerUrl();
    }
}
