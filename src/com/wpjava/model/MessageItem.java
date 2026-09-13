package com.wpjava.model;

import java.util.Calendar;
import java.util.Date;

/** View model for a chat message, including media and delivery state. */
public class MessageItem {

    public static final int STATUS_SENT = 1;
    public static final int STATUS_DELIVERED = 2;
    public static final int STATUS_READ = 3;

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_DOCUMENT = 3;

    public static final int AUDIO_IDLE = 0;
    public static final int AUDIO_DOWNLOADING = 1;
    public static final int AUDIO_PLAYING = 2;

    public boolean fromMe;
    public String text;
    public long timestamp;
    public int status;
    public int type;
    public int audioStatus = AUDIO_IDLE;
    public String audioUrl;
    public String audioMime;
    public int audioDurationSec;
    public int audioRecordId = -1;
    public String messageId;
    public String quotedText;
    public String sender;
    public int layoutH = -1;

    public MessageItem(boolean fromMe, String text, long timestamp, int status) {
        this.fromMe = fromMe;
        this.text = text;
        this.timestamp = timestamp;
        this.status = status;
        type = TYPE_TEXT;
    }

    public String getFormattedTime() {
        if (timestamp <= 0) return "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(asMilliseconds(timestamp)));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return (hour < 10 ? "0" : "") + hour + ":"
                + (minute < 10 ? "0" : "") + minute;
    }

    public String getPreview(int maxLength) {
        String value = text;
        if (value == null || value.length() == 0) {
            return type == TYPE_AUDIO ? "[Audio]" : "";
        }
        if (maxLength <= 0) return "";
        if (value.length() <= maxLength) return value;
        if (maxLength == 1) return "...";
        return value.substring(0, maxLength - 1) + "...";
    }

    private long asMilliseconds(long value) {
        return value < 100000000000L ? value * 1000L : value;
    }
}
