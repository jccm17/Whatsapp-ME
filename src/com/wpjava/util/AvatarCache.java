package com.wpjava.util;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Image;

/** Shared asynchronous cache for optional profile photographs. */
public final class AvatarCache {

    private static final Vector entries = new Vector();
    private static final Vector loading = new Vector();

    private AvatarCache() { }

    public static Image get(final String url, final int maxWidth, final int maxHeight,
            final Canvas target) {
        if (url == null || url.length() == 0) return null;
        final String key = url + "#" + maxWidth + "x" + maxHeight;
        synchronized (entries) {
            int i;
            for (i = 0; i < entries.size(); i++) {
                Entry entry = (Entry) entries.elementAt(i);
                if (key.equals(entry.key)) return entry.image;
            }
            for (i = 0; i < loading.size(); i++) {
                if (key.equals((String) loading.elementAt(i))) return null;
            }
            loading.addElement(key);
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] data = HttpUtil.getBytes(url);
                    Image source = Image.createImage(data, 0, data.length);
                    Image image = ImageUtil.scaleToFit(source, maxWidth, maxHeight);
                    synchronized (entries) {
                        entries.addElement(new Entry(key, image));
                    }
                } catch (Exception ignored) {
                } finally {
                    synchronized (entries) {
                        loading.removeElement(key);
                    }
                    if (target != null) target.repaint();
                }
            }
        }).start();
        return null;
    }

    private static class Entry {
        String key;
        Image image;

        Entry(String value, Image valueImage) {
            key = value;
            image = valueImage;
        }
    }
}
