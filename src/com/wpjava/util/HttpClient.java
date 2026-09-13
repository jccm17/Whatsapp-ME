package com.wpjava.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/** Strict HTTP helper that preserves response errors for callers that need them. */
public final class HttpClient {

    private HttpClient() { }

    public static String get(String url, String accessCode) throws Exception {
        HttpConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
            connection.setRequestMethod(HttpConnection.GET);
            configure(connection, accessCode, "application/json");
            int status = connection.getResponseCode();
            input = status >= 400 ? connection.openDataInputStream() : connection.openInputStream();
            String body = new String(readBytes(input), "UTF-8");
            if (status < 200 || status >= 300) throw new Exception(error(status, body));
            return body;
        } finally {
            close(input);
            close(connection);
        }
    }

    public static String post(String url, String body, String accessCode) throws Exception {
        HttpConnection connection = null;
        OutputStream output = null;
        InputStream input = null;
        try {
            connection = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
            connection.setRequestMethod(HttpConnection.POST);
            configure(connection, accessCode, "application/json");
            byte[] data = (body == null ? "{}" : body).getBytes("UTF-8");
            connection.setRequestProperty("Content-Length", String.valueOf(data.length));
            output = connection.openOutputStream();
            output.write(data);
            output.flush();
            int status = connection.getResponseCode();
            input = status >= 400 ? connection.openDataInputStream() : connection.openInputStream();
            String response = new String(readBytes(input), "UTF-8");
            if (status < 200 || status >= 300) throw new Exception(error(status, response));
            return response;
        } finally {
            close(output);
            close(input);
            close(connection);
        }
    }

    public static byte[] getBinary(String url, String accessCode) throws Exception {
        HttpConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
            connection.setRequestMethod(HttpConnection.GET);
            configure(connection, accessCode, null);
            int status = connection.getResponseCode();
            input = status >= 400 ? connection.openDataInputStream() : connection.openInputStream();
            byte[] data = readBytes(input);
            if (status < 200 || status >= 300) throw new Exception("HTTP " + status);
            return data;
        } finally {
            close(input);
            close(connection);
        }
    }

    public static byte[] decodeBase64(String value) {
        if (value == null) return new byte[0];
        int comma = value.indexOf(',');
        if (comma >= 0) value = value.substring(comma + 1);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int bits = 0;
        int bitCount = 0;
        int i;
        for (i = 0; i < value.length(); i++) {
            int digit = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                    .indexOf(value.charAt(i));
            if (digit < 0) continue;
            bits = (bits << 6) | digit;
            bitCount += 6;
            while (bitCount >= 8) {
                bitCount -= 8;
                output.write((bits >> bitCount) & 0xFF);
            }
        }
        return output.toByteArray();
    }

    private static void configure(HttpConnection connection, String accessCode,
            String contentType) throws Exception {
        if (contentType != null) connection.setRequestProperty("Content-Type", contentType);
        if (accessCode != null && accessCode.length() > 0) {
            connection.setRequestProperty("x-access-code", accessCode);
        }
        connection.setRequestProperty("Connection", "close");
    }

    private static String error(int status, String body) {
        String message = JsonParser.getString(body, "error");
        return message == null || message.length() == 0 ? "HTTP " + status : message;
    }

    private static byte[] readBytes(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private static void close(InputStream input) { try { if (input != null) input.close(); } catch (Exception ignored) { } }
    private static void close(OutputStream output) { try { if (output != null) output.close(); } catch (Exception ignored) { } }
    private static void close(HttpConnection connection) { try { if (connection != null) connection.close(); } catch (Exception ignored) { } }
}
