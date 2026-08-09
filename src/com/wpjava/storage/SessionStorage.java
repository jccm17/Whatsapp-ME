package com.wpjava.storage;

public class SessionStorage extends RecordStorage {

    private static final String STORE = "wp_session";

    public void saveToken(String token) {
        replace(STORE, token == null ? "" : token);
    }

    public String getToken() {
        String token = readFirst(STORE);
        if (token == null || token.length() == 0) {
            return null;
        }
        return token;
    }

    public boolean exists() {
        return getToken() != null;
    }

    public void clear() {
        clear(STORE);
    }
}
