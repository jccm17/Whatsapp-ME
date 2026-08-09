package com.wpjava.util;

import javax.microedition.lcdui.Image;

public class ImageUtil {

    private ImageUtil() {
    }

    public static Image scaleToFit(Image image, int maxWidth, int maxHeight) {
        if (image == null) {
            return null;
        }

        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        if (sourceWidth <= maxWidth && sourceHeight <= maxHeight) {
            return image;
        }

        int targetWidth = sourceWidth;
        int targetHeight = sourceHeight;
        if (targetWidth > maxWidth) {
            targetHeight = targetHeight * maxWidth / targetWidth;
            targetWidth = maxWidth;
        }
        if (targetHeight > maxHeight) {
            targetWidth = targetWidth * maxHeight / targetHeight;
            targetHeight = maxHeight;
        }
        if (targetWidth < 1) {
            targetWidth = 1;
        }
        if (targetHeight < 1) {
            targetHeight = 1;
        }

        int[] source = new int[sourceWidth * sourceHeight];
        int[] target = new int[targetWidth * targetHeight];
        image.getRGB(source, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        int x;
        int y;
        for (y = 0; y < targetHeight; y++) {
            int sourceY = y * sourceHeight / targetHeight;
            for (x = 0; x < targetWidth; x++) {
                int sourceX = x * sourceWidth / targetWidth;
                target[y * targetWidth + x] = source[sourceY * sourceWidth + sourceX];
            }
        }

        return Image.createRGBImage(target, targetWidth, targetHeight, false);
    }
}
