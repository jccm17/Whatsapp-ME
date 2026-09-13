package com.wpjava.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import com.wpjava.storage.Storage;

public class HttpUtil {

    public static String get(String url) {
        try {
            return HttpClient.get(url, Storage.getInstance().getAccessCode());
        } catch (Exception e) {
            return "";
        }
    }

    public static byte[] getBytes(String url) {
        try {
            return HttpClient.getBinary(url, Storage.getInstance().getAccessCode());
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static String post(String url, String body) {
        try {
            return HttpClient.post(url, body, Storage.getInstance().getAccessCode());
        } catch (Exception e) {
            return "";
        }
    }

    public static String postStream(String url, String contentType, InputStream input,
            long contentLength) {
        HttpConnection hc = null;
        OutputStream os = null;
        InputStream response = null;
        try {
            hc = (HttpConnection) Connector.open(url);
            hc.setRequestMethod(HttpConnection.POST);
            hc.setRequestProperty("Content-Type", contentType);
            hc.setRequestProperty("Content-Length", String.valueOf(contentLength));
            String accessCode = Storage.getInstance().getAccessCode();
            if (accessCode != null && accessCode.length() > 0) {
                hc.setRequestProperty("x-access-code", accessCode);
            }
            hc.setRequestProperty("Connection", "close");
            os = hc.openOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            os.flush();
            int code = hc.getResponseCode();
            response = code >= 400 ? hc.openDataInputStream() : hc.openInputStream();
            String body = new String(read(response), "UTF-8");
            return code >= 200 && code < 300 ? body : "";
        } catch (Exception e) {
            return "";
        } finally {
            close(os);
            close(response);
            close(hc);
        }
    }

    private static byte[] read(InputStream is) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) {
            bos.write(c);
        }
        return bos.toByteArray();
    }

    private static void close(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (Exception e) {
        }
    }

    private static void close(OutputStream os) {
        try {
            if (os != null) {
                os.close();
            }
        } catch (Exception e) {
        }
    }

    private static void close(HttpConnection hc) {
        try {
            if (hc != null) {
                hc.close();
            }
        } catch (Exception e) {
        }
    }
}
