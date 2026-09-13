package com.wpjava.service;

import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/** Uploads and then removes a captured JPEG file. */
public class ImageSender {

    public void send(final String recipient, final String path, final Listener listener) {
        new Thread(new Runnable() {
            public void run() {
                FileConnection file = null;
                InputStream input = null;
                boolean sent = false;
                String detail = null;
                try {
                    file = (FileConnection) Connector.open(path, Connector.READ);
                    input = file.openInputStream();
                    sent = new ApiClient().sendImage(recipient, input, file.fileSize());
                    if (!sent) detail = "El backend rechazo la imagen";
                } catch (Exception e) {
                    detail = e.getMessage() == null ? "No se pudo enviar la imagen" : e.getMessage();
                } finally {
                    try { if (input != null) input.close(); } catch (Exception ignored) { }
                    try { if (file != null) file.close(); } catch (Exception ignored) { }
                    delete(path);
                }
                if (listener != null) listener.onImageSent(sent, detail);
            }
        }).start();
    }

    private void delete(String path) {
        FileConnection file = null;
        try {
            file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {
        } finally {
            try { if (file != null) file.close(); } catch (Exception ignored) { }
        }
    }

    public interface Listener {
        void onImageSent(boolean sent, String detail);
    }
}
