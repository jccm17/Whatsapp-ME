package com.wpjava.service;

import java.util.Vector;

/** Small in-process event bus used to refresh visible MIDP canvases. */
public final class RealtimeEvents {

    private static Vector listeners = new Vector();

    private RealtimeEvents() { }

    public static synchronized void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.addElement(listener);
    }

    public static synchronized void removeListener(Listener listener) {
        listeners.removeElement(listener);
    }

    public static void messageReceived(String chatId) {
        Listener[] current = snapshot();
        int i;
        for (i = 0; i < current.length; i++) current[i].onRealtimeMessage(chatId);
    }

    public static void chatUpdated(String chatId) {
        Listener[] current = snapshot();
        int i;
        for (i = 0; i < current.length; i++) current[i].onRealtimeChat(chatId);
    }

    private static synchronized Listener[] snapshot() {
        Listener[] current = new Listener[listeners.size()];
        int i;
        for (i = 0; i < current.length; i++) current[i] = (Listener) listeners.elementAt(i);
        return current;
    }

    public interface Listener {
        void onRealtimeMessage(String chatId);
        void onRealtimeChat(String chatId);
    }
}
