package com.wpjava.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import com.wpjava.core.Config;
import com.wpjava.core.Router;
import com.wpjava.storage.PreferenceStorage;
import com.wpjava.util.ImageUtil;

public class SplashCanvas extends Canvas implements Runnable {

    private int progress;
    private Image icon;

    public SplashCanvas() {
        try {
            icon = ImageUtil.scaleToFit(
                    Image.createImage("/com/resources/icon.png"), 72, 72);
        } catch (Exception e) {
            icon = null;
        }
        new Thread(this).start();
    }

    public void run() {
        while (progress < 100) {
            progress += 5;
            repaint();
            serviceRepaints();
            try {
                Thread.sleep(90);
            } catch (Exception e) {
            }
        }
        if (new PreferenceStorage().isPermissionAccepted()) {
            Router.navigate(new LoadingScreen("Iniciando"));
        } else {
            Router.navigate(new PermissionScreen());
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(255, 255, 255);
        g.fillRect(0, 0, w, h);
        if (icon != null) {
            g.drawImage(icon, w / 2, 42, Graphics.HCENTER | Graphics.TOP);
        } else {
            g.setColor(18, 140, 126);
            g.fillRoundRect((w - 72) / 2, 44, 72, 72, 18, 18);
            g.setColor(255, 255, 255);
            g.drawString("WP", w / 2, 66, Graphics.HCENTER | Graphics.TOP);
        }
        g.setColor(18, 140, 126);
        g.drawString("WPJava", w / 2, 130, Graphics.HCENTER | Graphics.TOP);
        g.drawRect(30, 170, w - 60, 8);
        g.fillRect(30, 170, (w - 60) * progress / 100, 8);
        g.drawString("v" + Config.VERSION, w / 2, 198,
                Graphics.HCENTER | Graphics.TOP);
        g.drawString("By SESS-CORP", w / 2, h - 28,
                Graphics.HCENTER | Graphics.TOP);
    }
}
