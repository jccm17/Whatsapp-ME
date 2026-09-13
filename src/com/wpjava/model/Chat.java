package com.wpjava.model;

public class Chat {
    public String id;
    public String name;
    public String avatar;
    public String lastMessage;
    public long timestamp;
    public int unread;
    public boolean archived;

    public boolean isGroup() {
        return id != null && id.endsWith("@g.us");
    }
}
