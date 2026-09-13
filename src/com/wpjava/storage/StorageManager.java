package com.wpjava.storage;

public class StorageManager {

    public void clearAll() {
        new SessionStorage().clear();
        new ChatStorage().clear();
        new MessageStorage().clear();
        Storage.getInstance().clearAudio();
    }
}
