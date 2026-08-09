package com.wpjava.util;

import javax.microedition.lcdui.Image;

public class ImageCache {

    public Image fromBytes(byte[] data) {
        try {
            if (data != null && data.length > 0) {
                return Image.createImage(data, 0, data.length);
            }
        } catch (Exception e) {
        }
        return null;
    }
}
