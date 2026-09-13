package com.wpjava.service;

import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/** Line-delimited JSON TCP client for receiving backend events in real time. */
public class SocketManager {

    private static final int INITIAL_RECONNECT_DELAY = 3000;
    private static final int MAX_RECONNECT_DELAY = 30000;
    private static final int MAX_LINE_LENGTH = 4096;

    private String host;
    private int port;
    private String userId;
    private String accessCode;
    private EventListener listener;
    private StreamConnection connection;
    private InputStream input;
    private OutputStream output;
    private boolean running;
    private boolean connected;
    private int reconnectDelay = INITIAL_RECONNECT_DELAY;

    public SocketManager(String host, int port, String userId, String accessCode,
            EventListener listener) {
        this.host = host;
        this.port = port;
        this.userId = userId;
        this.accessCode = accessCode;
        this.listener = listener;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        reconnectDelay = INITIAL_RECONNECT_DELAY;
        new Thread(new Runnable() {
            public void run() { runLoop(); }
        }).start();
    }

    public synchronized void stop() {
        running = false;
        closeConnection();
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    private void runLoop() {
        while (isRunning()) {
            try {
                connect();
                readLoop();
            } catch (Exception ignored) {
            } finally {
                boolean wasConnected;
                synchronized (this) {
                    wasConnected = connected;
                    connected = false;
                }
                closeConnection();
                if (wasConnected && listener != null) listener.onDisconnected();
            }
            if (!isRunning()) break;
            try { Thread.sleep(reconnectDelay); } catch (Exception ignored) { }
            reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY);
        }
    }

    private void connect() throws Exception {
        String address = normalizedHost(host);
        connection = (StreamConnection) Connector.open("socket://" + address + ":" + port);
        input = connection.openInputStream();
        output = connection.openOutputStream();
        sendLine("{\"type\":\"auth\",\"userId\":\"" + escape(userId)
                + "\",\"code\":\"" + escape(accessCode) + "\"}");
        String response = readLine();
        if (response == null || response.indexOf("\"auth_ok\"") < 0) {
            throw new Exception("Autenticacion de socket rechazada");
        }
        synchronized (this) { connected = true; }
        reconnectDelay = INITIAL_RECONNECT_DELAY;
        if (listener != null) listener.onConnected();
    }

    private void readLoop() throws Exception {
        while (isRunning() && isConnected()) {
            String line = readLine();
            if (line == null) return;
            if (line.length() > 0) handleLine(line);
        }
    }

    private void handleLine(String line) {
        String type = jsonString(line, "type");
        if ("ping".equals(type)) {
            sendLine("{\"type\":\"pong\"}");
            return;
        }
        if (listener == null || type == null) return;
        if ("msg".equals(type)) {
            String chatId = jsonString(line, "chatId");
            if (chatId != null) listener.onMessage(chatId, value(jsonString(line, "text")),
                    jsonBoolean(line, "fromMe"), jsonLong(line, "ts"),
                    value(jsonString(line, "msgType")), value(jsonString(line, "msgId")),
                    jsonString(line, "sender"));
        } else if ("chat".equals(type)) {
            String chatId = jsonString(line, "chatId");
            if (chatId != null) listener.onChatUpdate(chatId, value(jsonString(line, "name")),
                    value(jsonString(line, "lastMessage")), jsonLong(line, "lastTimestamp"),
                    (int) jsonLong(line, "unreadCount"), jsonBoolean(line, "archived"));
        } else if ("ack".equals(type)) {
            listener.onAck(jsonString(line, "chatId"), jsonString(line, "msgId"),
                    (int) jsonLong(line, "status"));
        }
    }

    private synchronized boolean isRunning() { return running; }

    private synchronized void sendLine(String line) {
        try {
            if (output == null) return;
            byte[] data = (line + "\n").getBytes("UTF-8");
            output.write(data);
            output.flush();
        } catch (Exception ignored) {
            connected = false;
        }
    }

    private String readLine() throws Exception {
        StringBuffer line = new StringBuffer();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') return line.toString().trim();
            if (value != '\r') line.append((char) value);
            if (line.length() > MAX_LINE_LENGTH) throw new Exception("Evento demasiado grande");
        }
        return null;
    }

    private synchronized void closeConnection() {
        try { if (input != null) input.close(); } catch (Exception ignored) { }
        try { if (output != null) output.close(); } catch (Exception ignored) { }
        try { if (connection != null) connection.close(); } catch (Exception ignored) { }
        input = null;
        output = null;
        connection = null;
    }

    private static String normalizedHost(String value) {
        String result = value == null ? "" : value;
        if (result.startsWith("http://")) result = result.substring(7);
        else if (result.startsWith("https://")) result = result.substring(8);
        int slash = result.indexOf('/');
        if (slash >= 0) result = result.substring(0, slash);
        int colon = result.indexOf(':');
        if (colon >= 0) result = result.substring(0, colon);
        return result;
    }

    private static String jsonString(String json, String key) {
        String name = "\"" + key + "\"";
        int at = json.indexOf(name);
        if (at < 0) return null;
        int colon = json.indexOf(':', at + name.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length() || json.charAt(start) != '\"') return null;
        int end = start + 1;
        while (end < json.length()) {
            if (json.charAt(end) == '\\') { end += 2; continue; }
            if (json.charAt(end) == '\"') break;
            end++;
        }
        return end < json.length() ? unescape(json.substring(start + 1, end)) : null;
    }

    private static boolean jsonBoolean(String json, String key) {
        int at = json.indexOf("\"" + key + "\"");
        int colon = at < 0 ? -1 : json.indexOf(':', at);
        if (colon < 0) return false;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        return start < json.length() && json.charAt(start) == 't';
    }

    private static long jsonLong(String json, String key) {
        int at = json.indexOf("\"" + key + "\"");
        int colon = at < 0 ? -1 : json.indexOf(':', at);
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '-')) end++;
        try { return Long.parseLong(json.substring(start, end)); } catch (Exception ignored) { return 0; }
    }

    private static String unescape(String value) {
        StringBuffer result = new StringBuffer();
        int i;
        for (i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\\' && i + 1 < value.length()) {
                char escaped = value.charAt(++i);
                if (escaped == 'n') result.append('\n');
                else if (escaped == 'r') result.append('\r');
                else if (escaped == 't') result.append('\t');
                else result.append(escaped);
            } else result.append(current);
        }
        return result.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        StringBuffer result = new StringBuffer();
        int i;
        for (i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\"' || current == '\\') result.append('\\');
            if (current == '\n') result.append("\\n");
            else if (current == '\r') result.append("\\r");
            else result.append(current);
        }
        return result.toString();
    }

    private static String value(String input) { return input == null ? "" : input; }

    public interface EventListener {
        void onMessage(String chatId, String text, boolean fromMe, long timestamp,
                String messageType, String messageId, String sender);
        void onChatUpdate(String chatId, String name, String lastMessage,
                long timestamp, int unreadCount, boolean archived);
        void onAck(String chatId, String messageId, int status);
        void onConnected();
        void onDisconnected();
    }
}
