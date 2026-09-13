package com.wpjava.ui;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;

import com.wpjava.core.Router;
import com.wpjava.core.ScreenManager;
import com.wpjava.core.Theme;
import com.wpjava.model.Chat;
import com.wpjava.model.Message;
import com.wpjava.service.ApiClient;
import com.wpjava.service.AudioRecorderManager;
import com.wpjava.service.ImageSender;
import com.wpjava.service.RealtimeEvents;
import com.wpjava.storage.MessageStorage;
import com.wpjava.util.DateUtil;
import com.wpjava.util.EmojiUtil;
import com.wpjava.util.HttpUtil;
import com.wpjava.util.ImageUtil;
import com.wpjava.util.AvatarCache;

public class ChatScreen extends Canvas implements CommandListener, Runnable,
        AudioRecorderManager.RecordListener, ScreenManager.ScreenLifecycle,
        RealtimeEvents.Listener, EmoticonPickerScreen.Target {

    private static final int HEADER_HEIGHT = 44;
    private static final int INPUT_HEIGHT = 36;
    private static final int MESSAGE_TOP = HEADER_HEIGHT + 12;
    private static final int MESSAGE_BOTTOM_GAP = 8;
    private static final int DATE_SEPARATOR_HEIGHT = 22;
    private static final int BG_R = 236;
    private static final int BG_G = 229;
    private static final int BG_B = 221;

    private Command composeCommand = new Command("Enviar", Command.OK, 1);
    private Command refreshCommand = new Command("Actualizar", Command.SCREEN, 2);
    private Command backCommand = new Command("Atras", Command.BACK, 3);
    private Command sendTextCommand = new Command("Enviar", Command.OK, 1);
    private Command cancelTextCommand = new Command("Cancelar", Command.BACK, 2);
    private Command emoticonTextCommand = new Command(":-)", Command.SCREEN, 2);
    private Command recordCommand = new Command("Grabar audio", Command.SCREEN, 3);
    private Command stopRecordCommand = new Command("Detener audio", Command.STOP, 1);
    private Command cameraCommand = new Command("Tomar foto", Command.SCREEN, 4);

    private Chat chat;
    private MessageStorage storage;
    private Vector messages;
    private String pendingSend;
    private TextBox editor;
    private int scroll;
    private int selectedMessageIndex = -1;
    private int touchStartY = -1;
    private int lastTouchY = -1;
    private boolean dragging;
    private AudioRecorderManager recorder;
    private boolean sendingAudio;
    private boolean sendingImage;
    private Player mediaPlayer;
    private String playingMediaId;
    private Image viewingImage;
    private byte[] viewingImageData;
    private String viewingImageId;
    private String mediaNotice;
    private final Vector thumbnails = new Vector();
    private final Vector thumbnailLoading = new Vector();

    private Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    private Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private Font normalFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private Font smallFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    public ChatScreen(Chat chat) {
        this.chat = chat;
        storage = new MessageStorage();
        messages = storage.getMessages(chat.id);
        addCommand(composeCommand);
        addCommand(refreshCommand);
        addCommand(recordCommand);
        addCommand(stopRecordCommand);
        addCommand(cameraCommand);
        addCommand(backCommand);
        setCommandListener(this);
        scrollToBottom();
        new Thread(this).start();
    }

    public void commandAction(Command c, Displayable d) {
        if (d == editor) {
            if (c == sendTextCommand) {
                String text = editor.getString();
                editor = null;
                Router.replace(this);
                queueSend(text);
            } else if (c == emoticonTextCommand) {
                Router.getDisplay().setCurrent(new EmoticonPickerScreen(this));
            } else {
                editor = null;
                Router.replace(this);
            }
        } else if (c == composeCommand) {
            openComposer();
        } else if (c == refreshCommand) {
            new Thread(this).start();
        } else if (c == recordCommand) {
            startAudioRecording();
        } else if (c == stopRecordCommand) {
            stopAudioRecording();
        } else if (c == cameraCommand) {
            openCamera();
        } else if (c == backCommand) {
            if (recorder != null && recorder.isRecording()) {
                recorder.stopRecording();
            }
            goBack();
        }
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (viewingImage != null) {
            if (action == FIRE || keyCode == -6) {
                saveViewedImage();
            } else if (keyCode == -7 || keyCode == -11) {
                closeImageViewer();
            }
            return;
        }
        if (keyCode == -7 || keyCode == -11) {
            goBack();
            return;
        }
        if (action == UP) {
            moveMessageSelection(-1);
            repaint();
        } else if (action == DOWN) {
            moveMessageSelection(1);
            repaint();
        } else if (action == FIRE) {
            openSelectedMessage();
        }
    }

    protected void pointerPressed(int x, int y) {
        touchStartY = y;
        lastTouchY = y;
        dragging = false;
    }

    protected void pointerDragged(int x, int y) {
        if (lastTouchY < 0) return;
        int delta = lastTouchY - y;
        if (delta != 0) {
            scroll += delta;
            limitScroll();
            dragging = true;
            repaint();
        }
        lastTouchY = y;
    }

    protected void pointerReleased(int x, int y) {
        boolean tap = !dragging && touchStartY >= 0 && Math.abs(y - touchStartY) < 10;
        touchStartY = -1;
        lastTouchY = -1;
        if (viewingImage != null) {
            closeImageViewer();
            return;
        }
        if (!tap) return;
        if (y < HEADER_HEIGHT && x < 42) {
            goBack();
        } else if (y >= getHeight() - INPUT_HEIGHT) {
            if (x >= getWidth() - 48) {
                if (recorder != null && recorder.isRecording()) stopAudioRecording();
                else startAudioRecording();
            } else if (x >= getWidth() - 82) {
                openCamera();
            } else {
                openComposer();
            }
        } else {
            openMediaAt(y);
        }
    }

    private void goBack() {
        if (!Router.back()) {
            Router.replace(new ChatListScreen());
        }
    }

    public void run() {
        if (pendingSend != null) {
            sendPending();
            return;
        }

        Vector remote = new ApiClient().getMessages(chat.id);
        if (remote != null && remote.size() > 0) {
            messages = remote;
            storage.saveAll(messages);
            scrollToBottom();
            repaint();
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        if (viewingImage != null) {
            drawImageViewer(g, w, h);
            return;
        }

        drawBackground(g, w, h);
        drawHeader(g, w);
        drawMessages(g, w, h);
        drawScrollIndicator(g, w, h);
        drawHeader(g, w);
        drawInputBar(g, w, h);
    }

    private void drawBackground(Graphics g, int w, int h) {
        g.setColor(Theme.bgChat());
        g.fillRect(0, 0, w, h);

        g.setColor(Theme.isDark() ? 0x222222 : 0xE2DBD3);
        int x;
        int y;
        for (y = HEADER_HEIGHT + 8; y < h - INPUT_HEIGHT; y += 24) {
            for (x = 8; x < w; x += 36) {
                g.drawArc(x, y, 8, 8, 0, 360);
            }
        }
    }

    private void drawHeader(Graphics g, int w) {
        g.setColor(7, 94, 84);
        g.fillRect(0, 0, w, HEADER_HEIGHT);

        g.setColor(255, 255, 255);
        g.setFont(boldFont);
        g.drawString("<", 14, 14, Graphics.HCENTER | Graphics.TOP);

        g.setColor(chat.isGroup() ? 0x546E7A : 0x128C7E);
        g.fillArc(25, 7, 30, 30, 0, 360);
        Image avatar = chat.isGroup() ? null : AvatarCache.get(chat.avatar, 28, 28, this);
        if (avatar != null) {
            g.drawImage(avatar, 26, 8, Graphics.LEFT | Graphics.TOP);
        } else {
            g.setColor(255, 255, 255);
            g.setFont(boldFont);
            g.drawString(chat.isGroup() ? "G" : initial(chat.name), 40, 14,
                    Graphics.HCENTER | Graphics.TOP);
        }

        g.setFont(titleFont);
        g.drawString(shorten(chat.name, w - 86, titleFont), 62, 5,
                Graphics.LEFT | Graphics.TOP);
        g.setFont(smallFont);
        g.setColor(204, 235, 230);
        g.drawString(chat.isGroup() ? "Grupo" : shorten(chat.id, w - 86, smallFont), 62, 25,
                Graphics.LEFT | Graphics.TOP);
    }

    private void drawMessages(Graphics g, int w, int h) {
        int y = MESSAGE_TOP - scroll;
        int i;
        g.setFont(normalFont);
        for (i = 0; i < messages.size(); i++) {
            Message m = (Message) messages.elementAt(i);
            if (startsNewDay(i)) {
                if (y + DATE_SEPARATOR_HEIGHT >= MESSAGE_TOP
                        && y <= h - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP) {
                    drawDateSeparator(g, m.timestamp, y, w);
                }
                y += DATE_SEPARATOR_HEIGHT;
            }
            int height = bubbleHeight(m, w);
            if (y + height >= MESSAGE_TOP && y <= h - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP) {
                drawBubble(g, m, y, height, w, i);
            }
            y += height + 6;
        }
    }

    private void drawDateSeparator(Graphics g, long timestamp, int y, int width) {
        String text = DateUtil.conversationDate(timestamp);
        int boxWidth = smallFont.stringWidth(text) + 14;
        int x = (width - boxWidth) / 2;
        g.setColor(Theme.isDark() ? 0x303030 : 0xD8E6E2);
        g.fillRoundRect(x, y + 2, boxWidth, 16, 8, 8);
        g.setColor(Theme.textGray());
        g.setFont(smallFont);
        g.drawString(text, width / 2, y + 5, Graphics.HCENTER | Graphics.TOP);
    }

    private void drawScrollIndicator(Graphics g, int width, int height) {
        int viewport = height - MESSAGE_TOP - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP;
        int total = contentHeight(width);
        if (total <= viewport) return;
        int trackY = MESSAGE_TOP + 2;
        int trackH = viewport - 4;
        int thumbH = Math.max(14, trackH * viewport / total);
        int maxScroll = total - viewport;
        int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll;
        g.setColor(Theme.isDark() ? 0x555555 : 0xB0C8C1);
        g.fillRoundRect(width - 4, thumbY, 2, thumbH, 2, 2);
    }

    private void drawBubble(Graphics g, Message m, int y, int height, int w, int index) {
        boolean mine = !m.incoming;
        int maxWidth = w - 58;
        String body = EmojiUtil.toMessageDisplay(m.body);
        int bubbleWidth = Math.min(maxWidth, Math.max(64,
                textWidth(body, maxWidth - 16) + 18));
        if (hasMedia(m)) {
            bubbleWidth = Math.min(maxWidth,
                    Math.max(bubbleWidth, isImage(m) ? 170 : 112));
        }

        int x = mine ? w - bubbleWidth - 8 : 8;
        if (mine) {
            g.setColor(Theme.bubbleMine());
        } else {
            g.setColor(Theme.bubbleThem());
        }
        g.fillRoundRect(x, y, bubbleWidth, height, 8, 8);
        if (index == selectedMessageIndex) {
            g.setColor(Theme.selectedBorder());
            g.drawRoundRect(x - 1, y - 1, bubbleWidth + 2, height + 2, 10, 10);
            g.fillRect(mine ? x + bubbleWidth - 3 : x - 1, y + 9, 4, height - 18);
        } else {
            g.setColor(mine ? 196 : 226, mine ? 226 : 226, mine ? 182 : 226);
        }
        g.drawRoundRect(x, y, bubbleWidth, height, 8, 8);

        int textY = y + 7;
        if (hasMedia(m)) {
            int mediaHeight = mediaBlockHeight(m);
            g.setColor(238, 238, 238);
            g.fillRoundRect(x + 7, textY, bubbleWidth - 14, mediaHeight - 6, 6, 6);
            g.setColor(18, 140, 126);
            g.setFont(boldFont);
            if ("Imagen".equals(mediaLabel(m))) {
                Image thumbnail = getThumbnail(m.mediaUrl);
                if (thumbnail != null) {
                    g.drawImage(thumbnail, x + 11, textY + 3,
                            Graphics.LEFT | Graphics.TOP);
                } else {
                    g.drawRect(x + 13, textY + 8, 24, 20);
                    g.fillArc(x + 17, textY + 11, 6, 6, 0, 360);
                    g.drawLine(x + 14, textY + 27, x + 23, textY + 20);
                    g.drawLine(x + 23, textY + 20, x + 35, textY + 28);
                }
            } else if ("Audio".equals(mediaLabel(m))) {
                g.fillArc(x + 14, textY + 8, 22, 22, 0, 360);
                g.setColor(255, 255, 255);
                g.drawString(">", x + 25, textY + 12, Graphics.HCENTER | Graphics.TOP);
                g.setColor(18, 140, 126);
            }
            g.drawString(mediaLabel(m), x + (isImage(m) ? 92 : 43), textY + 14,
                    Graphics.LEFT | Graphics.TOP);
            if (m.id != null && m.id.equals(playingMediaId)) {
                g.setFont(smallFont);
                g.drawString("Centro: guardar", x + 43, textY + 27,
                        Graphics.LEFT | Graphics.TOP);
            }
            textY += mediaHeight;
        }

        g.setFont(normalFont);
        g.setColor(Theme.text());
        drawWrapped(g, body, x + 8, textY, bubbleWidth - 16);

        g.setFont(smallFont);
        g.setColor(Theme.textGray());
        String meta = DateUtil.time(m.timestamp);
        if (!m.incoming && m.status != null && m.status.length() > 0) {
            meta = meta + " " + statusMark(m.status);
        }
        g.drawString(meta, x + bubbleWidth - 7, y + height - 15,
                Graphics.RIGHT | Graphics.TOP);
    }

    private void drawInputBar(Graphics g, int w, int h) {
        int y = h - INPUT_HEIGHT;
        g.setColor(Theme.inputBg());
        g.fillRect(0, y, w, INPUT_HEIGHT);

        g.setColor(Theme.bubbleThem());
        g.fillRoundRect(8, y + 6, w - 100, 24, 12, 12);
        g.setColor(Theme.textGray());
        g.setFont(normalFont);
        g.drawString("Escribir mensaje", 20, y + 11, Graphics.LEFT | Graphics.TOP);

        g.setColor(Theme.textGray());
        g.drawRoundRect(w - 81, y + 11, 17, 12, 3, 3);
        g.fillRect(w - 76, y + 8, 7, 3);
        g.drawArc(w - 76, y + 14, 7, 7, 0, 360);

        boolean recording = recorder != null && recorder.isRecording();
        g.setColor(recording ? 220 : 37, recording ? 70 : 211, recording ? 70 : 102);
        g.fillArc(w - 46, y + 5, 26, 26, 0, 360);
        g.setColor(255, 255, 255);
        g.setFont(boldFont);
        if (recording) {
            g.drawString("[]", w - 33, y + 10, Graphics.HCENTER | Graphics.TOP);
            g.setColor(Theme.isDark() ? 0xFFFFFF : 0xD32F2F);
            g.drawString("Grabando " + seconds(recorder.getRecordedSeconds()) + " / "
                    + AudioRecorderManager.MAX_SECONDS + "s", 12, y - 13,
                    Graphics.LEFT | Graphics.TOP);
        } else if (sendingAudio || sendingImage) {
            g.drawString("...", w - 33, y + 10, Graphics.HCENTER | Graphics.TOP);
        } else {
            g.drawString("M", w - 33, y + 10, Graphics.HCENTER | Graphics.TOP);
        }
        if (mediaNotice != null) {
            g.setColor(Theme.textGray());
            g.setFont(smallFont);
            g.drawString(mediaNotice, 8, y - 13, Graphics.LEFT | Graphics.TOP);
        }
    }

    private void openComposer() {
        editor = new TextBox("Mensaje", "", 500, TextField.ANY);
        editor.addCommand(sendTextCommand);
        editor.addCommand(emoticonTextCommand);
        editor.addCommand(cancelTextCommand);
        editor.setCommandListener(this);
        Router.getDisplay().setCurrent(editor);
    }

    public void appendEmoticon(String value) {
        if (editor != null && value != null) {
            editor.insert(value, editor.getCaretPosition());
        }
    }

    public Displayable getEmoticonDisplayable() {
        if (editor == null) return this;
        return editor;
    }

    private void queueSend(String text) {
        if (isBlank(text)) {
            return;
        }
        pendingSend = text;
        Message m = new Message();
        m.id = String.valueOf(System.currentTimeMillis());
        m.chatId = chat.id;
        m.body = text;
        m.incoming = false;
        m.status = "sending";
        m.timestamp = System.currentTimeMillis();
        messages.addElement(m);
        scrollToBottom();
        repaint();
        new Thread(this).start();
    }

    private void startAudioRecording() {
        if (sendingAudio) {
            return;
        }
        if (recorder == null) {
            recorder = new AudioRecorderManager(this);
        }
        recorder.startRecording();
        repaint();
    }

    private void stopAudioRecording() {
        if (recorder != null) {
            recorder.stopRecording();
        }
        repaint();
    }

    private void openCamera() {
        if (sendingImage) return;
        Router.navigate(new CameraScreen(new CameraScreen.PhotoListener() {
            public void onPhotoReady(String path) {
                sendCapturedImage(path);
            }

            public void onPhotoCancelled() { }
        }));
    }

    private void sendCapturedImage(String path) {
        sendingImage = true;
        repaint();
        new ImageSender().send(chat.id, path, new ImageSender.Listener() {
            public void onImageSent(boolean sent, String detail) {
                sendingImage = false;
                if (sent) {
                    new Thread(ChatScreen.this).start();
                } else {
                    showError(detail == null ? "No se pudo enviar la imagen." : detail);
                }
                repaint();
            }
        });
    }

    public void onRecordTick(int seconds) {
        repaint();
    }

    public void onRecordStopped(boolean hasAudio) {
        repaint();
        if (hasAudio && recorder != null) {
            sendingAudio = true;
            repaint();
            recorder.sendAudio(chat.id, new ApiClient());
        } else if (!hasAudio) {
            showError("No se grabo audio. Intenta hablar al menos un segundo.");
        }
    }

    public void onSendResult(boolean sent, String detail) {
        sendingAudio = false;
        repaint();
        if (sent) {
            new Thread(this).start();
        } else {
            showError(detail == null ? "No se pudo enviar el audio." : detail);
        }
    }

    public void onResume() {
        RealtimeEvents.addListener(this);
    }

    public void onPause() {
        RealtimeEvents.removeListener(this);
        stopMediaPlayer();
    }

    public void onRealtimeMessage(String chatId) {
        if (ApiClient.normalizeRecipient(chat.id).equals(ApiClient.normalizeRecipient(chatId))) {
            messages = storage.getMessages(chat.id);
            scrollToBottom();
            repaint();
        }
    }

    public void onRealtimeChat(String chatId) { }

    private void sendPending() {
        String text = pendingSend;
        pendingSend = null;
        boolean sent = new ApiClient().sendMessage(chat.id, text);
        Message m = findLastLocal(text);
        if (sent) {
            if (m != null) {
                m.status = "sent";
                storage.save(m);
            }
            Vector remote = new ApiClient().getMessages(chat.id);
            if (remote != null && remote.size() > 0) {
                messages = remote;
                storage.saveAll(messages);
            }
            scrollToBottom();
            repaint();
        } else {
            if (m != null) {
                m.status = "failed";
            }
            repaint();
            Alert alert = new Alert("No enviado",
                    "El backend rechazo el envio. Revisa conexion o JID del chat.",
                    null, AlertType.ERROR);
            alert.setTimeout(3000);
            Router.getDisplay().setCurrent(alert, this);
        }
    }

    private Message findLastLocal(String text) {
        int i;
        for (i = messages.size() - 1; i >= 0; i--) {
            Message m = (Message) messages.elementAt(i);
            if (!m.incoming && text.equals(m.body)) {
                return m;
            }
        }
        return null;
    }

    private int bubbleHeight(Message m, int w) {
        int maxWidth = w - 74;
        int lines = countLines(EmojiUtil.toMessageDisplay(m.body), maxWidth);
        int height = 16 + lines * normalFont.getHeight() + 14;
        if (hasMedia(m)) {
            height += mediaBlockHeight(m);
        }
        return Math.max(30, height);
    }

    private int mediaBlockHeight(Message m) {
        return isImage(m) ? 72 : 48;
    }

    private int contentHeight(int w) {
        int total = 0;
        int i;
        for (i = 0; i < messages.size(); i++) {
            if (startsNewDay(i)) total += DATE_SEPARATOR_HEIGHT;
            total += bubbleHeight((Message) messages.elementAt(i), w) + 6;
        }
        return total;
    }

    private void moveMessageSelection(int delta) {
        if (messages == null || messages.size() == 0) {
            return;
        }
        if (selectedMessageIndex < 0 || selectedMessageIndex >= messages.size()) {
            selectedMessageIndex = delta < 0 ? messages.size() - 1 : 0;
        } else {
            selectedMessageIndex += delta;
            if (selectedMessageIndex < 0) {
                selectedMessageIndex = 0;
            } else if (selectedMessageIndex >= messages.size()) {
                selectedMessageIndex = messages.size() - 1;
            }
        }
        ensureMessageVisible();
    }

    private void ensureMessageVisible() {
        if (selectedMessageIndex < 0 || selectedMessageIndex >= messages.size()) {
            return;
        }
        int y = MESSAGE_TOP - scroll;
        int i;
        for (i = 0; i < selectedMessageIndex; i++) {
            if (startsNewDay(i)) y += DATE_SEPARATOR_HEIGHT;
            y += bubbleHeight((Message) messages.elementAt(i), getWidth()) + 6;
        }
        if (startsNewDay(selectedMessageIndex)) y += DATE_SEPARATOR_HEIGHT;
        int height = bubbleHeight((Message) messages.elementAt(selectedMessageIndex), getWidth());
        int bottom = getHeight() - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP;
        if (y < MESSAGE_TOP) {
            scroll += y - MESSAGE_TOP;
        } else if (y + height > bottom) {
            scroll += y + height - bottom;
        }
        limitScroll();
    }

    private void scrollToBottom() {
        scroll = contentHeight(getWidth())
                - (getHeight() - MESSAGE_TOP - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP);
        if (scroll < 0) {
            scroll = 0;
        }
    }

    private void limitScroll() {
        int max = contentHeight(getWidth())
                - (getHeight() - MESSAGE_TOP - INPUT_HEIGHT - MESSAGE_BOTTOM_GAP);
        if (max < 0) {
            max = 0;
        }
        if (scroll > max) {
            scroll = max;
        }
        if (scroll < 0) {
            scroll = 0;
        }
    }

    private int countLines(String text, int maxWidth) {
        if (text == null || text.length() == 0) {
            return 1;
        }
        int lines = 1;
        int width = 0;
        int i;
        for (i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int cw = EmojiUtil.isInlineIcon(ch) ? 13 : normalFont.charWidth(ch);
            if (ch == '\n' || width + cw > maxWidth) {
                lines++;
                width = 0;
            }
            if (ch != '\n') {
                width += cw;
            }
        }
        return lines;
    }

    private int textWidth(String text, int maxWidth) {
        if (text == null || text.length() == 0) {
            return normalFont.stringWidth("Mensaje");
        }
        int max = 0;
        int width = 0;
        int i;
        for (i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int cw = EmojiUtil.isInlineIcon(ch) ? 13 : normalFont.charWidth(ch);
            if (ch == '\n' || width + cw > maxWidth) {
                if (width > max) {
                    max = width;
                }
                width = 0;
            }
            if (ch != '\n') {
                width += cw;
            }
        }
        return width > max ? width : max;
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxWidth) {
        if (text == null || text.length() == 0) {
            text = "";
        }
        StringBuffer pending = new StringBuffer();
        int pendingWidth = 0;
        int lineWidth = 0;
        int lineY = y;
        int i;
        for (i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int cw = EmojiUtil.isInlineIcon(ch) ? 13 : normalFont.charWidth(ch);
            if (ch == '\n' || lineWidth + cw > maxWidth) {
                if (pending.length() > 0) {
                    g.drawString(pending.toString(), x + lineWidth - pendingWidth, lineY,
                            Graphics.LEFT | Graphics.TOP);
                }
                pending.setLength(0);
                pendingWidth = 0;
                lineWidth = 0;
                lineY += normalFont.getHeight();
            }
            if (ch == '\n') continue;
            if (EmojiUtil.isInlineIcon(ch)) {
                if (pending.length() > 0) {
                    g.drawString(pending.toString(), x + lineWidth - pendingWidth, lineY,
                            Graphics.LEFT | Graphics.TOP);
                    pending.setLength(0);
                    pendingWidth = 0;
                }
                drawInlineEmoji(g, ch, x + lineWidth, lineY);
                lineWidth += cw;
            } else {
                pending.append(ch);
                pendingWidth += cw;
                lineWidth += cw;
            }
        }
        if (pending.length() > 0) {
            g.drawString(pending.toString(), x + lineWidth - pendingWidth, lineY,
                    Graphics.LEFT | Graphics.TOP);
        }
    }

    private void drawInlineEmoji(Graphics g, char icon, int x, int y) {
        if (icon == EmojiUtil.ICON_HOT_FACE) {
            g.setColor(0xF57C00);
            g.fillArc(x, y, 12, 12, 0, 360);
            g.setColor(0x5D1A00);
            g.fillRect(x + 3, y + 4, 2, 2);
            g.fillRect(x + 8, y + 4, 2, 2);
            g.drawLine(x + 3, y + 9, x + 9, y + 9);
            g.setColor(0xFFFFFF);
            g.fillArc(x + 9, y - 2, 4, 5, 0, 360);
        } else if (icon == EmojiUtil.ICON_KISS_FACE) {
            g.setColor(0xFFC107);
            g.fillArc(x, y, 12, 12, 0, 360);
            g.setColor(0x5D3A00);
            g.fillRect(x + 3, y + 4, 2, 2);
            g.fillArc(x + 7, y + 7, 3, 2, 0, 360);
            g.setColor(0xE91E63);
            g.fillArc(x + 10, y + 3, 4, 4, 0, 360);
        } else if (icon == EmojiUtil.ICON_SLEEP) {
            g.setColor(0x5C6BC0);
            g.setFont(smallFont);
            g.drawString("Zz", x, y, Graphics.LEFT | Graphics.TOP);
        } else if (icon == EmojiUtil.ICON_EYES) {
            g.setColor(0xFFFFFF);
            g.fillArc(x, y + 3, 6, 7, 0, 360);
            g.fillArc(x + 7, y + 3, 6, 7, 0, 360);
            g.setColor(0x263238);
            g.fillArc(x + 3, y + 5, 2, 3, 0, 360);
            g.fillArc(x + 10, y + 5, 2, 3, 0, 360);
        } else if (icon == EmojiUtil.ICON_UP || icon == EmojiUtil.ICON_DOWN) {
            g.setColor(0xFFCC80);
            g.fillRoundRect(x + 4, y + 2, 5, 9, 3, 3);
            g.fillRect(x + 1, icon == EmojiUtil.ICON_UP ? y : y + 8, 10, 4);
        } else if (icon == EmojiUtil.ICON_SKULL) {
            g.setColor(0xECEFF1);
            g.fillArc(x + 1, y, 11, 11, 0, 360);
            g.fillRect(x + 3, y + 8, 7, 4);
            g.setColor(0x37474F);
            g.fillArc(x + 3, y + 4, 2, 2, 0, 360);
            g.fillArc(x + 8, y + 4, 2, 2, 0, 360);
        } else if (icon == EmojiUtil.ICON_CIGARETTE) {
            g.setColor(0xFAFAFA);
            g.fillRect(x, y + 6, 10, 3);
            g.setColor(0xFF7043);
            g.fillRect(x + 10, y + 6, 3, 3);
        } else {
            drawFaceEmoji(g, icon, x, y);
        }
    }

    private void drawFaceEmoji(Graphics g, char icon, int x, int y) {
        g.setColor(icon == EmojiUtil.ICON_CRY ? 0x90CAF9 : 0xFFCA28);
        g.fillArc(x, y, 12, 12, 0, 360);
        g.setColor(0x5D3A00);
        g.fillRect(x + 3, y + 4, 2, 2);
        if (icon == EmojiUtil.ICON_WINK) g.drawLine(x + 8, y + 5, x + 10, y + 5);
        else g.fillRect(x + 8, y + 4, 2, 2);
        if (icon == EmojiUtil.ICON_LOVE) {
            g.setColor(0xE91E63);
            g.fillArc(x + 2, y + 3, 3, 3, 0, 360);
            g.fillArc(x + 7, y + 3, 3, 3, 0, 360);
        } else if (icon == EmojiUtil.ICON_COOL) {
            g.setColor(0x263238);
            g.fillRect(x + 2, y + 3, 8, 3);
        } else if (icon == EmojiUtil.ICON_TONGUE) {
            g.setColor(0xE91E63);
            g.fillRect(x + 5, y + 8, 3, 3);
        } else if (icon == EmojiUtil.ICON_THINK) {
            g.drawLine(x + 2, y + 3, x + 5, y + 2);
            g.drawLine(x + 7, y + 2, x + 10, y + 3);
        } else if (icon == EmojiUtil.ICON_CRY) {
            g.setColor(0x2196F3);
            g.fillRect(x + 8, y + 6, 2, 4);
        }
        g.setColor(0x5D3A00);
        if (icon == EmojiUtil.ICON_LAUGH) g.fillRect(x + 3, y + 8, 6, 2);
        else g.drawLine(x + 3, y + 9, x + 9, y + 9);
    }

    private void openMediaAt(int screenY) {
        int y = MESSAGE_TOP - scroll;
        int i;
        for (i = 0; i < messages.size(); i++) {
            Message message = (Message) messages.elementAt(i);
            if (startsNewDay(i)) y += DATE_SEPARATOR_HEIGHT;
            int height = bubbleHeight(message, getWidth());
            if (screenY >= y && screenY <= y + height) {
                selectedMessageIndex = i;
                if (hasMedia(message)) {
                    if (isAudio(message)) openAudio(message);
                    else if (isImage(message)) viewImage(message);
                    else downloadDocument(message);
                } else {
                    repaint();
                }
                return;
            }
            y += height + 6;
        }
    }

    private boolean startsNewDay(int index) {
        if (index < 0 || index >= messages.size()) return false;
        if (index == 0) return true;
        Message current = (Message) messages.elementAt(index);
        Message previous = (Message) messages.elementAt(index - 1);
        return !DateUtil.sameDay(previous.timestamp, current.timestamp);
    }

    /** Uses the centre key on the focused message; otherwise opens the composer. */
    private void openSelectedMessage() {
        if (selectedMessageIndex >= 0 && selectedMessageIndex < messages.size()) {
            Message message = (Message) messages.elementAt(selectedMessageIndex);
            if (hasMedia(message)) {
                if (isAudio(message)) openAudio(message);
                else if (isImage(message)) viewImage(message);
                else downloadDocument(message);
                return;
            }
        }
        openComposer();
    }

    private void playAudio(final Message message) {
        mediaNotice = "Descargando audio...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] data = HttpUtil.getBytes(message.mediaUrl);
                    if (data.length == 0) throw new Exception();
                    stopMediaPlayer();
                    mediaPlayer = Manager.createPlayer(new ByteArrayInputStream(data), message.mediaType);
                    mediaPlayer.realize();
                    mediaPlayer.start();
                    playingMediaId = message.id;
                    mediaNotice = "Reproduciendo audio";
                } catch (Exception e) {
                    mediaNotice = "No se pudo reproducir el audio";
                }
                repaint();
            }
        }).start();
    }

    private void openAudio(Message message) {
        if (message.id != null && message.id.equals(playingMediaId)) {
            downloadAudio(message);
        } else {
            playAudio(message);
        }
    }

    private void downloadAudio(final Message message) {
        mediaNotice = "Descargando audio...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                FileConnection file = null;
                OutputStream output = null;
                String path = "file:///E:/wpjava_" + safeFilePart(message.id)
                        + audioExtension(message.mediaType);
                try {
                    byte[] data = HttpUtil.getBytes(message.mediaUrl);
                    if (data.length == 0) throw new Exception();
                    try {
                        file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
                        if (file.exists()) file.delete();
                        file.create();
                    } catch (Exception first) {
                        try { if (file != null) file.close(); } catch (Exception ignored) { }
                        path = "file:///C:/wpjava_" + safeFilePart(message.id)
                                + audioExtension(message.mediaType);
                        file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
                        if (file.exists()) file.delete();
                        file.create();
                    }
                    output = file.openOutputStream();
                    output.write(data);
                    output.flush();
                    mediaNotice = "Audio guardado en " + (path.indexOf("E:") >= 0 ? "E:" : "C:");
                } catch (Exception e) {
                    mediaNotice = "No se pudo guardar el audio";
                } finally {
                    try { if (output != null) output.close(); } catch (Exception ignored) { }
                    try { if (file != null) file.close(); } catch (Exception ignored) { }
                    repaint();
                }
            }
        }).start();
    }

    private void viewImage(final Message message) {
        mediaNotice = "Descargando imagen...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] data = HttpUtil.getBytes(message.mediaUrl);
                    Image image = Image.createImage(data, 0, data.length);
                    viewingImage = ImageUtil.scaleToFit(image, getWidth() - 12, getHeight() - 30);
                    viewingImageData = data;
                    viewingImageId = message.id;
                    mediaNotice = null;
                } catch (Exception e) {
                    mediaNotice = "No se pudo abrir la imagen";
                }
                repaint();
            }
        }).start();
    }

    private void downloadDocument(final Message message) {
        mediaNotice = "Descargando archivo...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                FileConnection file = null;
                OutputStream output = null;
                try {
                    byte[] data = HttpUtil.getBytes(message.mediaUrl);
                    if (data.length == 0) throw new Exception();
                    String path = "file:///E:/wpjava_download.bin";
                    try {
                        file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
                        if (file.exists()) file.delete();
                        file.create();
                    } catch (Exception e) {
                        try { if (file != null) file.close(); } catch (Exception ignored) { }
                        path = "file:///C:/wpjava_download.bin";
                        file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
                        if (file.exists()) file.delete();
                        file.create();
                    }
                    output = file.openOutputStream();
                    output.write(data);
                    output.flush();
                    mediaNotice = "Guardado en " + (path.indexOf("E:") >= 0 ? "E:" : "C:");
                } catch (Exception e) {
                    mediaNotice = "No se pudo descargar el archivo";
                } finally {
                    try { if (output != null) output.close(); } catch (Exception ignored) { }
                    try { if (file != null) file.close(); } catch (Exception ignored) { }
                    repaint();
                }
            }
        }).start();
    }

    private void drawImageViewer(Graphics g, int width, int height) {
        g.setColor(0x000000);
        g.fillRect(0, 0, width, height);
        g.drawImage(viewingImage, width / 2, height / 2, Graphics.HCENTER | Graphics.VCENTER);
        g.setColor(0xFFFFFF);
        g.setFont(smallFont);
        g.drawString("Centro: guardar  |  Toca: cerrar", width / 2, height - 16,
                Graphics.HCENTER | Graphics.TOP);
        if (mediaNotice != null) {
            g.drawString(mediaNotice, width / 2, 8, Graphics.HCENTER | Graphics.TOP);
        }
    }

    private void closeImageViewer() {
        viewingImage = null;
        viewingImageData = null;
        viewingImageId = null;
        repaint();
    }

    private void saveViewedImage() {
        final byte[] data = viewingImageData;
        if (data == null || data.length == 0) {
            return;
        }
        mediaNotice = "Guardando imagen...";
        repaint();
        new Thread(new Runnable() {
            public void run() {
                FileConnection file = null;
                OutputStream output = null;
                String name = "wpjava_" + safeFilePart(viewingImageId) + ".jpg";
                String path = "file:///E:/" + name;
                try {
                    try {
                        file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
                        if (file.exists()) file.delete();
                        file.create();
                    } catch (Exception first) {
                        try { if (file != null) file.close(); } catch (Exception ignored) { }
                        path = "file:///C:/" + name;
                        file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
                        if (file.exists()) file.delete();
                        file.create();
                    }
                    output = file.openOutputStream();
                    output.write(data);
                    output.flush();
                    mediaNotice = "Imagen guardada en " + (path.indexOf("E:") >= 0 ? "E:" : "C:");
                } catch (Exception e) {
                    mediaNotice = "No se pudo guardar la imagen";
                } finally {
                    try { if (output != null) output.close(); } catch (Exception ignored) { }
                    try { if (file != null) file.close(); } catch (Exception ignored) { }
                    repaint();
                }
            }
        }).start();
    }

    private void stopMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.close();
            }
        } catch (Exception ignored) { }
        mediaPlayer = null;
        playingMediaId = null;
    }

    private Image getThumbnail(final String url) {
        if (url == null || url.length() == 0) return null;
        synchronized (thumbnails) {
            int i;
            for (i = 0; i < thumbnails.size(); i++) {
                Thumbnail item = (Thumbnail) thumbnails.elementAt(i);
                if (url.equals(item.url)) return item.image;
            }
            for (i = 0; i < thumbnailLoading.size(); i++) {
                if (url.equals((String) thumbnailLoading.elementAt(i))) return null;
            }
            thumbnailLoading.addElement(url);
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] data = HttpUtil.getBytes(url);
                    Image source = Image.createImage(data, 0, data.length);
                    Image preview = ImageUtil.scaleToFit(source, 72, 54);
                    synchronized (thumbnails) {
                        thumbnails.addElement(new Thumbnail(url, preview));
                    }
                } catch (Exception ignored) {
                } finally {
                    synchronized (thumbnails) {
                        thumbnailLoading.removeElement(url);
                    }
                    repaint();
                }
            }
        }).start();
        return null;
    }

    private String safeFilePart(String value) {
        if (value == null || value.length() == 0) return "imagen";
        StringBuffer out = new StringBuffer();
        int i;
        for (i = 0; i < value.length() && out.length() < 24; i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')) out.append(c);
        }
        return out.length() == 0 ? "imagen" : out.toString();
    }

    private String audioExtension(String mime) {
        if (mime != null && mime.indexOf("ogg") >= 0) return ".ogg";
        if (mime != null && mime.indexOf("mp3") >= 0) return ".mp3";
        return ".amr";
    }

    private static class Thumbnail {
        String url;
        Image image;

        Thumbnail(String value, Image valueImage) {
            url = value;
            image = valueImage;
        }
    }

    private boolean hasMedia(Message m) {
        return m.mediaUrl != null && m.mediaUrl.length() > 0;
    }

    private boolean isAudio(Message m) {
        return m.mediaType != null && m.mediaType.indexOf("audio/") == 0;
    }

    private boolean isImage(Message m) {
        return m.mediaType != null && m.mediaType.indexOf("image/") == 0;
    }

    private String mediaLabel(Message m) {
        if (m.mediaType != null && m.mediaType.indexOf("audio/") == 0) {
            return "Audio";
        }
        if (m.mediaType != null && m.mediaType.indexOf("video/") == 0) {
            return "Video";
        }
        if (m.mediaType != null && m.mediaType.indexOf("application/") == 0) {
            return "Archivo";
        }
        return "Imagen";
    }

    private String seconds(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private void showError(String message) {
        Alert alert = new Alert("Adjunto", message, null, AlertType.ERROR);
        alert.setTimeout(3000);
        Router.getDisplay().setCurrent(alert, this);
    }

    private String statusMark(String status) {
        if ("sending".equals(status)) {
            return "...";
        }
        if ("failed".equals(status)) {
            return "!";
        }
        return "ok";
    }

    private boolean isBlank(String text) {
        if (text == null) {
            return true;
        }
        int i;
        for (i = 0; i < text.length(); i++) {
            if (text.charAt(i) > ' ') {
                return false;
            }
        }
        return true;
    }

    private String initial(String value) {
        if (value == null || value.length() == 0) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }

    private String shorten(String value, int maxWidth, Font font) {
        if (value == null) {
            value = "";
        }
        if (font.stringWidth(value) <= maxWidth) {
            return value;
        }
        while (value.length() > 0 && font.stringWidth(value + "...") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "...";
    }
}
