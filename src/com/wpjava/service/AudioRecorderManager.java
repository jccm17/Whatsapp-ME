package com.wpjava.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.RecordControl;

import com.wpjava.util.TempMediaFile;

/** Captures one short voice note using MMAPI and keeps it until it is sent. */
public class AudioRecorderManager {

    public static final int MAX_SECONDS = 30;
    private Player player;
    private RecordControl recordControl;
    private String tempFilePath;
    private Timer timer;
    private int recordedSeconds;
    private boolean recording;
    private boolean stopRequested;
    private RecordListener listener;

    public AudioRecorderManager(RecordListener listener) {
        this.listener = listener;
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized int getRecordedSeconds() {
        return recordedSeconds;
    }

    public synchronized void startRecording() {
        if (recording) {
            return;
        }
        recording = true;
        stopRequested = false;
        recordedSeconds = 0;
        new Thread(new Runnable() {
            public void run() {
                try {
                    prepareTempFile();
                    player = createAudioPlayer();
                    player.realize();
                    recordControl = (RecordControl) player.getControl("RecordControl");
                    if (recordControl == null) {
                        throw new Exception("El telefono no ofrece RecordControl");
                    }
                    recordControl.setRecordLocation(tempFilePath);
                    try {
                        recordControl.setRecordSizeLimit(-1);
                    } catch (Exception ignored) {
                    }
                    recordControl.startRecord();
                    player.start();

                    synchronized (AudioRecorderManager.this) {
                        if (stopRequested) {
                            finishRecording(false);
                            return;
                        }
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            public void run() {
                                tick();
                            }
                        }, 1000, 1000);
                    }
                } catch (Exception e) {
                    synchronized (AudioRecorderManager.this) {
                        recording = false;
                    }
                    cleanupResources();
                    notifyFailure("No se pudo abrir el microfono: " + message(e));
                }
            }
        }).start();
    }

    public synchronized void stopRecording() {
        if (!recording) {
            return;
        }
        stopRequested = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (recordControl != null) {
            new Thread(new Runnable() {
                public void run() {
                    synchronized (AudioRecorderManager.this) {
                        finishRecording(recordedSeconds > 0);
                    }
                }
            }).start();
        }
    }

    public void sendAudio(final String recipient, final ApiClient api) {
        new Thread(new Runnable() {
            public void run() {
                FileConnection file = null;
                InputStream input = null;
                try {
                    if (tempFilePath == null) {
                        throw new Exception("No hay audio para enviar");
                    }
                    file = (FileConnection) Connector.open(tempFilePath, Connector.READ);
                    input = file.openInputStream();
                    boolean sent = api.sendAudio(recipient, input, file.fileSize(),
                            detectMimeType(tempFilePath));
                    if (listener != null) {
                        listener.onSendResult(sent, sent ? null : "El backend rechazo el audio");
                    }
                } catch (Exception e) {
                    notifyFailure("No se pudo enviar el audio: " + message(e));
                } finally {
                    close(input);
                    close(file);
                    deleteTempFile();
                }
            }
        }).start();
    }

    public synchronized void deleteTempFile() {
        if (tempFilePath == null) {
            return;
        }
        FileConnection file = null;
        try {
            file = (FileConnection) Connector.open(tempFilePath, Connector.READ_WRITE);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
        } finally {
            close(file);
            tempFilePath = null;
        }
    }

    private void prepareTempFile() throws Exception {
        tempFilePath = TempMediaFile.prepare("voice.amr");
    }

    private Player createAudioPlayer() throws Exception {
        String[] locators = { "capture://audio?encoding=audio/amr", "capture://audio" };
        Exception last = null;
        int i;
        for (i = 0; i < locators.length; i++) {
            try {
                return Manager.createPlayer(locators[i]);
            } catch (Exception e) {
                last = e;
            }
        }
        throw last == null ? new Exception("Captura de audio no disponible") : last;
    }

    private String detectMimeType(String path) {
        FileConnection file = null;
        InputStream input = null;
        try {
            file = (FileConnection) Connector.open(path, Connector.READ);
            input = file.openInputStream();
            byte[] header = new byte[12];
            int read = input.read(header);
            if (read >= 6 && header[0] == '#' && header[1] == '!'
                    && header[2] == 'A' && header[3] == 'M' && header[4] == 'R') {
                return read >= 9 && header[6] == '-' && header[7] == 'W' ? "audio/amr-wb" : "audio/amr";
            }
            if (read >= 8 && header[4] == 'f' && header[5] == 't'
                    && header[6] == 'y' && header[7] == 'p') {
                return "audio/3gpp";
            }
        } catch (Exception ignored) {
        } finally {
            close(input);
            close(file);
        }
        return "audio/3gpp";
    }

    private synchronized void tick() {
        if (!recording || stopRequested) {
            return;
        }
        recordedSeconds++;
        if (listener != null) {
            listener.onRecordTick(recordedSeconds);
        }
        if (recordedSeconds >= MAX_SECONDS) {
            stopRecording();
        }
    }

    private void finishRecording(boolean completed) {
        if (!recording) {
            return;
        }
        recording = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        try {
            if (recordControl != null) {
                recordControl.stopRecord();
                recordControl.commit();
            }
            if (player != null) {
                player.stop();
            }
        } catch (Exception e) {
            completed = false;
        } finally {
            cleanupResources();
        }
        if (listener != null) {
            listener.onRecordStopped(completed);
        }
    }

    private void cleanupResources() {
        try {
            if (player != null) {
                player.close();
            }
        } catch (Exception ignored) {
        }
        player = null;
        recordControl = null;
    }

    private void notifyFailure(String detail) {
        if (listener != null) {
            listener.onSendResult(false, detail);
        }
    }

    private String message(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    private void close(InputStream input) {
        try { if (input != null) input.close(); } catch (Exception ignored) { }
    }

    private void close(FileConnection file) {
        try { if (file != null) file.close(); } catch (Exception ignored) { }
    }

    public interface RecordListener {
        void onRecordTick(int seconds);
        void onRecordStopped(boolean hasAudio);
        void onSendResult(boolean sent, String detail);
    }
}
