package com.wpjava.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class HttpUtil {

    public static String get(String url) {
        byte[] bytes = getBytes(url);
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            return new String(bytes);
        }
    }

    public static byte[] getBytes(String url) {
        HttpConnection hc = null;
        InputStream is = null;
        try {
            hc = (HttpConnection) Connector.open(url);
            hc.setRequestMethod(HttpConnection.GET);
            is = hc.openInputStream();
            return read(is);
        } catch (Exception e) {
            return new byte[0];
        } finally {
            close(is);
            close(hc);
        }
    }

    public static String post(String url, String body) {
        HttpConnection hc = null;
        OutputStream os = null;
        InputStream is = null;
        try {
            hc = (HttpConnection) Connector.open(url);
            hc.setRequestMethod(HttpConnection.POST);
            hc.setRequestProperty("Content-Type", "application/json");
            byte[] data = body == null ? new byte[0] : body.getBytes("UTF-8");
            hc.setRequestProperty("Content-Length", String.valueOf(data.length));
            os = hc.openOutputStream();
            os.write(data);
            os.flush();
            os.close();
            os = null;
            is = hc.openInputStream();
            return new String(read(is), "UTF-8");
        } catch (Exception e) {
            return "";
        } finally {
            close(os);
            close(is);
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
