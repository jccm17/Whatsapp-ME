package com.wpjava.ui;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;

import com.wpjava.core.Router;
import com.wpjava.core.ScreenManager;
import com.wpjava.util.TempMediaFile;

/** Camera preview that saves one JPEG before returning it to the caller. */
public class CameraScreen extends Canvas implements ScreenManager.ScreenLifecycle {

    private static final int CAPTURE_BAR_HEIGHT = 82;

    private PhotoListener listener;
    private Player player;
    private VideoControl videoControl;
    private boolean captured;
    private boolean capturing;
    private String savedPath;
    private String statusMessage;

    public CameraScreen(PhotoListener listener) {
        this.listener = listener;
        setFullScreenMode(true);
    }

    public void onResume() { startCamera(); }
    public void onPause() { stopCamera(); }

    protected void sizeChanged(final int width, final int height) {
        repaint();
        if (videoControl == null) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    videoControl.setDisplayLocation(0, 0);
                    videoControl.setDisplaySize(width, height - CAPTURE_BAR_HEIGHT);
                    videoControl.setVisible(true);
                } catch (Exception ignored) { }
            }
        }).start();
    }

    protected void paint(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        g.setColor(0x000000);
        g.fillRect(0, 0, width, height);
        if (captured) {
            g.setColor(0x075E84);
            g.fillRect(0, 0, width, height);
            g.setColor(0xFFFFFF);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
            g.drawString("Foto capturada", width / 2, height / 2 - 20,
                    Graphics.HCENTER | Graphics.TOP);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            g.drawString("Izq: Enviar   Der: Descartar", width / 2, height / 2 + 10,
                    Graphics.HCENTER | Graphics.TOP);
            return;
        }
        int centerX = width / 2;
        int centerY = height - CAPTURE_BAR_HEIGHT / 2;
        g.setColor(0x101010);
        g.fillRect(0, height - CAPTURE_BAR_HEIGHT, width, CAPTURE_BAR_HEIGHT);
        g.setColor(0xFFFFFF);
        g.drawArc(centerX - 25, centerY - 25, 50, 50, 0, 360);
        g.setColor(capturing ? 0xE53935 : 0xFFFFFF);
        g.fillArc(centerX - 19, centerY - 19, 38, 38, 0, 360);
        g.setColor(0x075E84);
        g.fillRoundRect(centerX - 10, centerY - 6, 20, 14, 3, 3);
        g.fillRect(centerX - 5, centerY - 10, 10, 4);
        g.setColor(0xFFFFFF);
        g.fillArc(centerX - 4, centerY - 3, 8, 8, 0, 360);
        g.setColor(0xFFFFFF);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString(capturing ? "Capturando..." : "Capturar", width / 2, height - 10,
                Graphics.HCENTER | Graphics.TOP);
        if (statusMessage != null) {
            g.setColor(0xCC0000);
            g.drawString(statusMessage, 4, height - CAPTURE_BAR_HEIGHT - 16,
                    Graphics.LEFT | Graphics.TOP);
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (!captured) {
            if (keyCode == -6 || action == FIRE) capturePhoto();
            else if (keyCode == -7) cancel();
        } else if (keyCode == -6 || action == FIRE) {
            if (listener != null) listener.onPhotoReady(savedPath);
            Router.back();
        } else if (keyCode == -7) {
            deleteTempFile();
            cancel();
        }
    }

    protected void pointerPressed(int x, int y) {
        if (!captured && isCaptureButton(x, y)) capturePhoto();
        else if (x < getWidth() / 2) {
            if (listener != null) listener.onPhotoReady(savedPath);
            Router.back();
        } else {
            deleteTempFile();
            cancel();
        }
    }

    private void startCamera() {
        if (player != null || captured) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    player = createCameraPlayer();
                    player.realize();
                    videoControl = (VideoControl) player.getControl("VideoControl");
                    if (videoControl == null) throw new Exception("VideoControl no disponible");
                    videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, CameraScreen.this);
                    layoutPreview(getWidth(), getHeight());
                    videoControl.setVisible(true);
                    player.start();
                    repaint();
                } catch (Exception e) {
                    statusMessage = "Camara no disponible: " + detail(e);
                    stopCamera();
                    repaint();
                }
            }
        }).start();
    }

    private Player createCameraPlayer() throws Exception {
        String[] locators = { "capture://image", "capture://video" };
        Exception last = null;
        int i;
        for (i = 0; i < locators.length; i++) {
            try {
                return Manager.createPlayer(locators[i]);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last == null ? new Exception("MMAPI no disponible") : last;
    }

    private void capturePhoto() {
        if (captured || capturing || videoControl == null) return;
        capturing = true;
        statusMessage = null;
        repaint();
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] image;
                    try {
                        image = videoControl.getSnapshot("encoding=jpeg&width=240&height=320");
                    } catch (Exception e) {
                        image = videoControl.getSnapshot(null);
                    }
                    savedPath = saveImage(image);
                    captured = true;
                    stopCamera();
                } catch (Exception e) {
                    statusMessage = "No se pudo tomar la foto";
                } finally {
                    capturing = false;
                    repaint();
                }
            }
        }).start();
    }

    private String saveImage(byte[] image) throws Exception {
        return TempMediaFile.write("photo.jpg", image);
    }

    private void cancel() {
        stopCamera();
        if (listener != null) listener.onPhotoCancelled();
        Router.back();
    }

    private void deleteTempFile() {
        if (savedPath == null) return;
        FileConnection file = null;
        try {
            file = (FileConnection) Connector.open(savedPath, Connector.READ_WRITE);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {
        } finally {
            try { if (file != null) file.close(); } catch (Exception ignored) { }
            savedPath = null;
        }
    }

    private void stopCamera() {
        try { if (videoControl != null) videoControl.setVisible(false); } catch (Exception ignored) { }
        try { if (player != null) { player.stop(); player.close(); } } catch (Exception ignored) { }
        videoControl = null;
        player = null;
    }

    private void layoutPreview(int width, int height) throws Exception {
        videoControl.setDisplayLocation(0, 0);
        try { videoControl.setDisplayFullScreen(false); } catch (Exception ignored) { }
        videoControl.setDisplaySize(width, height - CAPTURE_BAR_HEIGHT);
        videoControl.setVisible(true);
    }

    private boolean isCaptureButton(int x, int y) {
        int dx = x - getWidth() / 2;
        int dy = y - (getHeight() - CAPTURE_BAR_HEIGHT / 2);
        return dx * dx + dy * dy <= 34 * 34;
    }

    private String detail(Exception e) {
        String value = e.getMessage();
        return value == null || value.length() == 0 ? "MMAPI" : value;
    }

    public interface PhotoListener {
        void onPhotoReady(String path);
        void onPhotoCancelled();
    }
}
