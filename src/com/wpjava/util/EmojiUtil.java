package com.wpjava.util;

public class EmojiUtil {

    private EmojiUtil() {
    }

    public static String toDisplay(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }

        StringBuffer out = new StringBuffer();
        int i = 0;
        while (i < value.length()) {
            char ch = value.charAt(i);
            if (ch == 0x00F0 && i + 3 < value.length()) {
                out.append(mojibakeEmoji(value.charAt(i + 3)));
                i += 4;
            } else if (isHighSurrogate(ch) && i + 1 < value.length()) {
                out.append(surrogateEmoji(ch, value.charAt(i + 1)));
                i += 2;
            } else if (ch == 0x2764) {
                out.append("<3");
                if (i + 1 < value.length() && value.charAt(i + 1) == 0xFE0F) {
                    i++;
                }
                i++;
            } else if (ch == 0x263A) {
                out.append(":-)");
                i++;
            } else if (ch == 0x2665) {
                out.append("<3");
                i++;
            } else if (ch == 0x2605) {
                out.append("*");
                i++;
            } else {
                out.append(ch);
                i++;
            }
        }
        return out.toString();
    }

    private static String surrogateEmoji(char high, char low) {
        if (high == 0xD83D) {
            if (low == 0xDE05) {
                return ":-D";
            }
            if (low == 0xDE02) {
                return "XD";
            }
            if (low == 0xDE00 || low == 0xDE03 || low == 0xDE04 || low == 0xDE0A) {
                return ":-)";
            }
            if (low == 0xDE09) {
                return ";-)";
            }
            if (low == 0xDE0D) {
                return "<3";
            }
            if (low == 0xDE18) {
                return ":*";
            }
            if (low == 0xDE22 || low == 0xDE2D) {
                return ":'(";
            }
            if (low == 0xDE14 || low == 0xDE1E) {
                return ":-(";
            }
            if (low == 0xDE0E) {
                return "B-)";
            }
            if (low == 0xDC4D) {
                return "ok";
            }
            if (low == 0xDE4F) {
                return "gracias";
            }
        }
        return "?";
    }

    private static String mojibakeEmoji(char last) {
        if (last == 0x2026 || last == 0x0085) {
            return ":-D";
        }
        if (last == 0x201A || last == 0x0082) {
            return "XD";
        }
        if (last == 0x02DC || last == 0x0098) {
            return ":-)";
        }
        return ":-)";
    }

    private static boolean isHighSurrogate(char ch) {
        return ch >= 0xD800 && ch <= 0xDBFF;
    }
}
