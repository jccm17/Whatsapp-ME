package com.wpjava.storage;

public class PreferenceStorage extends RecordStorage {

    private static final String STORE_PERMISSION = "wp_permission";

    public void savePermissionAccepted() {
        replace(STORE_PERMISSION, "1");
    }

    public boolean isPermissionAccepted() {
        String value = readFirst(STORE_PERMISSION);
        return "1".equals(value);
    }
}
