package com.wpjava.storage;

import javax.microedition.rms.RecordStore;

import com.wpjava.core.Config;

/** Persistent application configuration and locally retained voice notes. */
public class Storage {

    private static final String STORE_CONFIG = "wp_config";
    private static final String STORE_AUDIO = "wp_audio";
    private static final char SEP = '|';
    private static Storage instance;

    private String userId = "";
    private String accessCode = "";
    private String serverUrl = Config.API_URL.trim();
    private boolean disclaimerShown;
    private boolean darkMode;

    public static synchronized Storage getInstance() {
        if (instance == null) {
            instance = new Storage();
        }
        return instance;
    }

    private Storage() {
        load();
    }

    public String getUserId() { return userId; }
    public String getAccessCode() { return accessCode; }
    public String getServerUrl() { return serverUrl; }
    public boolean isDisclaimerShown() { return disclaimerShown; }
    public boolean isDarkMode() { return darkMode; }

    public void setUserId(String value) { userId = value == null ? "" : value; }
    public void setAccessCode(String value) { accessCode = value == null ? "" : value; }
    public void setServerUrl(String value) {
        serverUrl = value == null || value.length() == 0 ? Config.API_URL.trim() : value;
    }
    public void setDisclaimerShown(boolean value) { disclaimerShown = value; }
    public void setDarkMode(boolean value) { darkMode = value; }

    public void save() {
        RecordStore store = null;
        try {
            byte[] data = encode().getBytes("UTF-8");
            store = RecordStore.openRecordStore(STORE_CONFIG, true);
            if (store.getNumRecords() == 0) {
                store.addRecord(data, 0, data.length);
            } else {
                store.setRecord(1, data, 0, data.length);
            }
        } catch (Exception ignored) {
        } finally {
            close(store);
        }
    }

    public int saveAudio(byte[] audio) {
        RecordStore store = null;
        try {
            if (audio == null || audio.length == 0) {
                return -1;
            }
            store = RecordStore.openRecordStore(STORE_AUDIO, true);
            return store.addRecord(audio, 0, audio.length);
        } catch (Exception ignored) {
            return -1;
        } finally {
            close(store);
        }
    }

    public byte[] loadAudio(int id) {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE_AUDIO, false);
            return store.getRecord(id);
        } catch (Exception ignored) {
            return null;
        } finally {
            close(store);
        }
    }

    public void clearAudio() {
        try { RecordStore.deleteRecordStore(STORE_AUDIO); } catch (Exception ignored) { }
    }

    private void load() {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(STORE_CONFIG, false);
            if (store.getNumRecords() > 0) {
                parse(new String(store.getRecord(1), "UTF-8"));
            }
        } catch (Exception ignored) {
        } finally {
            close(store);
        }
    }

    private String encode() {
        return safe(userId) + SEP + safe(accessCode) + SEP + safe(serverUrl) + SEP
                + (disclaimerShown ? "1" : "0") + SEP + (darkMode ? "1" : "0");
    }

    private void parse(String value) {
        String[] values = split(value);
        if (values.length > 0) userId = values[0];
        if (values.length > 1) accessCode = values[1];
        if (values.length > 2 && values[2].length() > 0) serverUrl = values[2];
        if (values.length > 3) disclaimerShown = "1".equals(values[3]);
        if (values.length > 4) darkMode = "1".equals(values[4]);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(SEP, ' ');
    }

    private String[] split(String value) {
        int count = 1;
        int i;
        for (i = 0; i < value.length(); i++) if (value.charAt(i) == SEP) count++;
        String[] values = new String[count];
        int start = 0;
        int index = 0;
        for (i = 0; i < value.length(); i++) {
            if (value.charAt(i) == SEP) {
                values[index++] = value.substring(start, i);
                start = i + 1;
            }
        }
        values[index] = value.substring(start);
        return values;
    }

    private void close(RecordStore store) {
        try { if (store != null) store.closeRecordStore(); } catch (Exception ignored) { }
    }
}
