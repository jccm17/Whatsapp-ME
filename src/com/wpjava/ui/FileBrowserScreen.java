package com.wpjava.ui;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import com.wpjava.core.Router;
import com.wpjava.core.ScreenManager;
import com.wpjava.core.Theme;

/** Touch and keypad file picker for JSR-75 file-system devices. */
public class FileBrowserScreen extends Canvas implements ScreenManager.ScreenLifecycle {

    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 28;

    private FileListener listener;
    private String currentPath;
    private Vector items = new Vector();
    private int selectedIndex;
    private int scrollOffset;
    private String statusMessage;

    public FileBrowserScreen(FileListener listener) {
        this.listener = listener;
        setFullScreenMode(true);
    }

    public void onResume() { loadRoots(); }
    public void onPause() { }

    private void loadRoots() {
        items.removeAllElements();
        try {
            Enumeration roots = FileSystemRegistry.listRoots();
            while (roots.hasMoreElements()) items.addElement("file:///" + roots.nextElement());
            if (items.size() == 1) {
                currentPath = (String) items.elementAt(0);
                loadDirectory(currentPath);
                return;
            }
            currentPath = "file:///";
            selectedIndex = 0;
            scrollOffset = 0;
            repaint();
        } catch (Exception e) {
            statusMessage = "No hay almacenamiento disponible";
            repaint();
        }
    }

    private void loadDirectory(final String path) {
        statusMessage = "Cargando...";
        items.removeAllElements();
        selectedIndex = 0;
        scrollOffset = 0;
        repaint();
        new Thread(new Runnable() {
            public void run() {
                Vector result = new Vector();
                FileConnection directory = null;
                try {
                    directory = (FileConnection) Connector.open(path, Connector.READ);
                    Enumeration names = directory.list();
                    while (names.hasMoreElements()) result.addElement(names.nextElement());
                    if (!isRoot(path)) result.insertElementAt("../", 0);
                    items = result;
                    statusMessage = null;
                } catch (Exception e) {
                    statusMessage = "No se pudo abrir la carpeta";
                } finally {
                    try { if (directory != null) directory.close(); } catch (Exception ignored) { }
                    repaint();
                }
            }
        }).start();
    }

    protected void paint(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        g.setColor(Theme.bgList());
        g.fillRect(0, 0, width, height);
        g.setColor(0x075E84);
        g.fillRect(0, 0, width, HEADER_HEIGHT);
        g.setColor(0xFFFFFF);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString(shortPath(), width / 2, 13, Graphics.HCENTER | Graphics.TOP);

        int contentHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int visible = contentHeight / ITEM_HEIGHT;
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + visible) scrollOffset = selectedIndex - visible + 1;
        int i;
        for (i = scrollOffset; i < items.size(); i++) {
            int y = HEADER_HEIGHT + (i - scrollOffset) * ITEM_HEIGHT;
            if (y >= HEADER_HEIGHT + contentHeight) break;
            if (i == selectedIndex) {
                g.setColor(Theme.hover());
                g.fillRect(0, y, width, ITEM_HEIGHT);
            }
            String item = (String) items.elementAt(i);
            boolean folder = item.endsWith("/");
            g.setColor(folder ? 0x075E84 : Theme.text());
            g.setFont(Font.getFont(Font.FACE_SYSTEM, folder ? Font.STYLE_BOLD : Font.STYLE_PLAIN,
                    Font.SIZE_SMALL));
            g.drawString(folder ? "[" + item + "]" : item, 7, y + 8,
                    Graphics.LEFT | Graphics.TOP);
            g.setColor(Theme.sep());
            g.drawLine(0, y + ITEM_HEIGHT - 1, width, y + ITEM_HEIGHT - 1);
        }
        if (statusMessage != null) {
            g.setColor(Theme.textGray());
            g.drawString(statusMessage, width / 2, HEADER_HEIGHT + contentHeight / 2,
                    Graphics.HCENTER | Graphics.TOP);
        }
        g.setColor(Theme.inputBg());
        g.fillRect(0, height - FOOTER_HEIGHT, width, FOOTER_HEIGHT);
        g.setColor(Theme.text());
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("Selec", 5, height - 17, Graphics.LEFT | Graphics.TOP);
        g.drawString("Cancelar", width - 5, height - 17, Graphics.RIGHT | Graphics.TOP);
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == UP && selectedIndex > 0) { selectedIndex--; repaint(); }
        else if (action == DOWN && selectedIndex < items.size() - 1) { selectedIndex++; repaint(); }
        else if (action == FIRE || keyCode == -6) select(selectedIndex);
        else if (keyCode == -7) cancel();
    }

    protected void pointerPressed(int x, int y) {
        if (y >= getHeight() - FOOTER_HEIGHT) { cancel(); return; }
        if (y < HEADER_HEIGHT) { if (!isRoot(currentPath)) openParent(); return; }
        int index = scrollOffset + (y - HEADER_HEIGHT) / ITEM_HEIGHT;
        if (index >= 0 && index < items.size()) {
            if (index == selectedIndex) select(index);
            else { selectedIndex = index; repaint(); }
        }
    }

    private void select(int index) {
        if (index < 0 || index >= items.size()) return;
        String item = (String) items.elementAt(index);
        if (item.equals("../")) { openParent(); return; }
        if (currentPath.equals("file:///")) {
            currentPath = item;
            loadDirectory(currentPath);
        } else if (item.endsWith("/")) {
            currentPath = currentPath + item;
            loadDirectory(currentPath);
        } else if (listener != null) {
            listener.onFileSelected(currentPath + item, item);
        }
    }

    private void openParent() {
        String value = currentPath;
        if (value == null || isRoot(value)) { loadRoots(); return; }
        if (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        int slash = value.lastIndexOf('/');
        currentPath = slash <= 7 ? "file:///" : value.substring(0, slash + 1);
        if (currentPath.equals("file:///")) loadRoots(); else loadDirectory(currentPath);
    }

    private boolean isRoot(String value) {
        if (value == null || value.equals("file:///")) return true;
        String rest = value.substring("file:///".length());
        return rest.indexOf('/') == rest.length() - 1;
    }

    private String shortPath() {
        String value = currentPath == null ? "Archivos" : currentPath.substring(8);
        return value.length() > 25 ? "..." + value.substring(value.length() - 22) : value;
    }

    private void cancel() {
        if (listener != null) listener.onFileCancelled();
        Router.back();
    }

    public interface FileListener {
        void onFileSelected(String path, String name);
        void onFileCancelled();
    }
}
