package com.wpjava.util;

public class EmojiUtil {

    public static final char ICON_HOT_FACE = '\u0001';
    public static final char ICON_KISS_FACE = '\u0002';
    public static final char ICON_SMILE = '\u0003';
    public static final char ICON_LAUGH = '\u0004';
    public static final char ICON_LOVE = '\u0005';
    public static final char ICON_WINK = '\u0006';
    public static final char ICON_TONGUE = '\u0007';
    public static final char ICON_COOL = '\u0008';
    public static final char ICON_SLEEP = '\u0009';
    public static final char ICON_EYES = '\u0011';
    public static final char ICON_UP = '\u0012';
    public static final char ICON_DOWN = '\u0013';
    public static final char ICON_SKULL = '\u0014';
    public static final char ICON_CIGARETTE = '\u0015';
    public static final char ICON_THINK = '\u0016';
    public static final char ICON_CRY = '\u0017';

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

    /** Preserves selected modern emojis as compact Canvas icon markers. */
    public static String toMessageDisplay(String value) {
        if (value == null || value.length() == 0) return "";
        StringBuffer out = new StringBuffer();
        int i = 0;
        while (i < value.length()) {
            char high = value.charAt(i);
            if (isHighSurrogate(high) && i + 1 < value.length()) {
                char low = value.charAt(i + 1);
                if (high == 0xD83C && low >= 0xDFFB && low <= 0xDFFF) {
                    i += 2;
                    continue;
                }
                char icon = iconFor(high, low);
                if (icon == 0) {
                    out.append(surrogateEmoji(high, low));
                } else {
                    out.append(icon);
                }
                i += 2;
            } else if (high == 0x263A) {
                out.append(ICON_SMILE);
                if (i + 1 < value.length() && value.charAt(i + 1) == 0xFE0F) i++;
                i++;
            } else {
                out.append(high);
                i++;
            }
        }
        return out.toString();
    }

    public static boolean isInlineIcon(char value) {
        return value == ICON_HOT_FACE || value == ICON_KISS_FACE
                || (value >= ICON_SMILE && value <= ICON_SLEEP)
                || (value >= ICON_EYES && value <= ICON_THINK)
                || value == ICON_CRY;
    }

    private static char iconFor(char high, char low) {
        if (high == 0xD83D) {
            if (low == 0xDE02 || low == 0xDE03 || low == 0xDE04 || low == 0xDE05
                    || low == 0xDE06 || low == 0xDE01) return ICON_LAUGH;
            if (low == 0xDE0D) return ICON_LOVE;
            if (low == 0xDE18 || low == 0xDE17 || low == 0xDE19 || low == 0xDE1A) return ICON_KISS_FACE;
            if (low == 0xDE09) return ICON_WINK;
            if (low == 0xDE0B || low == 0xDE1B || low == 0xDE1D) return ICON_TONGUE;
            if (low == 0xDE0E || low == 0xDE13) return ICON_COOL;
            if (low == 0xDE0C || low == 0xDE0A || low == 0xDE07 || low == 0xDE42 || low == 0xDE43 || low == 0xDE00) return ICON_SMILE;
            if (low == 0xDC4D) return ICON_UP;
            if (low == 0xDC4E) return ICON_DOWN;
            if (low == 0xDC40) return ICON_EYES;
            if (low == 0xDCA4) return ICON_SLEEP;
            if (low == 0xDC80) return ICON_SKULL;
            if (low == 0xDEAC) return ICON_CIGARETTE;
        }
        if (high == 0xD83E) {
            if (low == 0xDD75) return ICON_HOT_FACE;
            if (low == 0xDD79 || low == 0xDD72) return ICON_CRY;
            if (low == 0xDD70) return ICON_LOVE;
            if (low == 0xDD2A) return ICON_TONGUE;
            if (low == 0xDDD0 || low == 0xDD28) return ICON_THINK;
            if (low == 0xDD23) return ICON_LAUGH;
        }
        return 0;
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
