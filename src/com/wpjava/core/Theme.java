package com.wpjava.core;

import com.wpjava.storage.Storage;

/** Central light/dark palette persisted in RMS. */
public final class Theme {

    private static boolean dark;

    private Theme() { }

    public static void init() { dark = Storage.getInstance().isDarkMode(); }
    public static boolean isDark() { return dark; }
    public static void setDark(boolean value) {
        dark = value;
        Storage.getInstance().setDarkMode(value);
        Storage.getInstance().save();
    }
    public static int bgList() { return dark ? 0x111111 : 0xFFFFFF; }
    public static int bgChat() { return dark ? 0x1A1A1A : 0xECE5DD; }
    public static int bubbleMine() { return dark ? 0x005C4B : 0xDCF8C6; }
    public static int bubbleThem() { return dark ? 0x2A2A2A : 0xFFFFFF; }
    public static int text() { return dark ? 0xE0E0E0 : 0x000000; }
    public static int textGray() { return dark ? 0xAAAAAA : 0x888888; }
    public static int sep() { return dark ? 0x333333 : 0xE0E0E0; }
    public static int hover() { return dark ? 0x222222 : 0xF5F5F5; }
    public static int selected() { return dark ? 0x164A42 : 0xBDEFE2; }
    public static int selectedBorder() { return dark ? 0x4FC3A1 : 0x00796B; }
    public static int inputBg() { return dark ? 0x1E1E1E : 0xF0F0F0; }
    public static int splashBg() { return dark ? 0x000000 : 0xFFFFFF; }
    public static int splashText() { return dark ? 0x4FC3A1 : 0x075E84; }
}
