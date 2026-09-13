package com.wpjava.model;

import java.util.Calendar;
import java.util.Date;

import javax.microedition.lcdui.Font;

/** View model for a conversation row in the chat list. */
public class ChatItem {

    public String id;
    public String name;
    public String lastMessage;
    public long lastTimestamp;
    public int unreadCount;
    public String avatarUrl;

    private long formattedTimestamp = -1;
    private String formattedTime;
    private String nameCacheSource;
    private int nameCacheWidth = -1;
    private String nameCache;
    private String previewCacheSource;
    private int previewCacheWidth = -1;
    private String previewCache;

    public ChatItem(String id, String name, String lastMessage, long lastTimestamp,
            int unreadCount) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.unreadCount = unreadCount;
    }

    public String getDisplayName() {
        if (name != null && name.trim().length() > 0) return name.trim();
        if (id == null) return "?";
        if (id.endsWith("@s.whatsapp.net")) {
            return "+" + id.substring(0, id.length() - "@s.whatsapp.net".length());
        }
        if (id.endsWith("@lid")) {
            String value = id.substring(0, id.length() - "@lid".length());
            if (value.length() > 8) value = value.substring(value.length() - 8);
            return "~" + value;
        }
        if (id.endsWith("@g.us")) return "Grupo";
        return id;
    }

    public String getFormattedTime() {
        if (lastTimestamp <= 0) return "";
        if (formattedTime != null && formattedTimestamp == lastTimestamp) {
            return formattedTime;
        }
        long milliseconds = asMilliseconds(lastTimestamp);
        long elapsed = System.currentTimeMillis() - milliseconds;
        long day = 24L * 60L * 60L * 1000L;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(milliseconds));
        String result;
        if (elapsed >= 0 && elapsed < day) {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            result = (hour < 10 ? "0" : "") + hour + ":"
                    + (minute < 10 ? "0" : "") + minute;
        } else if (elapsed >= day && elapsed < 2L * day) {
            result = "ayer";
        } else {
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1;
            result = (dayOfMonth < 10 ? "0" : "") + dayOfMonth + "/"
                    + (month < 10 ? "0" : "") + month;
        }
        formattedTimestamp = lastTimestamp;
        formattedTime = result;
        return result;
    }

    public String getTruncatedName(Font font, int width) {
        String source = getDisplayName();
        if (nameCache != null && same(nameCacheSource, source) && nameCacheWidth == width) {
            return nameCache;
        }
        nameCache = truncate(source, font, width);
        nameCacheSource = source;
        nameCacheWidth = width;
        return nameCache;
    }

    public String getTruncatedPreview(Font font, int width) {
        String source = lastMessage == null ? "" : lastMessage;
        if (previewCache != null && same(previewCacheSource, source)
                && previewCacheWidth == width) {
            return previewCache;
        }
        previewCache = truncate(source, font, width);
        previewCacheSource = source;
        previewCacheWidth = width;
        return previewCache;
    }

    private static String truncate(String value, Font font, int width) {
        if (value == null || width <= 0) return "";
        if (font.stringWidth(value) <= width) return value;
        String suffix = "...";
        while (value.length() > 0 && font.stringWidth(value + suffix) > width) {
            value = value.substring(0, value.length() - 1);
        }
        return value + suffix;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static long asMilliseconds(long value) {
        return value < 100000000000L ? value * 1000L : value;
    }
}
