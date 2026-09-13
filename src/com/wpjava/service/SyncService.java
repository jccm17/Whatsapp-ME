package com.wpjava.service;

import com.wpjava.core.Config;
import com.wpjava.model.Chat;
import com.wpjava.model.Message;
import com.wpjava.storage.ChatStorage;
import com.wpjava.storage.MessageStorage;
import com.wpjava.storage.Storage;

public class SyncService implements Runnable {

    private boolean running;
    private Thread thread;
    private ApiClient api;
    private SocketManager socket;

    public SyncService() {
        api = new ApiClient();
    }

    public void start() {
        if (thread == null) {
            running = true;
            startSocket();
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.stop();
            socket = null;
        }
        thread = null;
    }

    public void run() {
        while (running) {
            try {
                api.sync();
                Thread.sleep(Config.SYNC_INTERVAL);
            } catch (Exception e) {
            }
        }
    }

    private void startSocket() {
        Storage storage = Storage.getInstance();
        socket = new SocketManager(storage.getServerUrl(), Config.SOCKET_PORT,
                storage.getUserId(), storage.getAccessCode(), new SocketManager.EventListener() {
            public void onMessage(String chatId, String text, boolean fromMe, long timestamp,
                    String messageType, String messageId, String sender) {
                Message message = new Message();
                message.id = messageId;
                message.chatId = chatId;
                message.body = text;
                message.incoming = !fromMe;
                message.timestamp = timestamp;
                message.type = messageType;
                message.status = fromMe ? "sent" : "";
                new MessageStorage().save(message);
                RealtimeEvents.messageReceived(chatId);
            }

            public void onChatUpdate(String chatId, String name, String lastMessage,
                    long timestamp, int unreadCount, boolean archived) {
                Chat chat = new Chat();
                chat.id = chatId;
                chat.name = name;
                chat.lastMessage = lastMessage;
                chat.timestamp = timestamp;
                chat.unread = unreadCount;
                chat.archived = archived;
                new ChatStorage().saveChat(chat);
                RealtimeEvents.chatUpdated(chatId);
            }

            public void onAck(String chatId, String messageId, int status) {
                String value = status == 3 ? "read" : (status == 2 ? "delivered" : "sent");
                new MessageStorage().updateStatus(messageId, value);
            }
            public void onConnected() { }
            public void onDisconnected() { }
        });
        socket.start();
    }
}
