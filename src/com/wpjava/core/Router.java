package com.wpjava.core;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import com.wpjava.WPJavaMidlet;

public class Router {

    private static ScreenManager screenManager;

    private Router() {
    }

    public static void init(WPJavaMidlet midlet) {
        screenManager = new ScreenManager(midlet);
    }

    public static void navigate(Displayable screen) {
        if (screenManager != null) {
            screenManager.show(screen);
        }
    }

    public static void replace(Displayable screen) {
        if (screenManager != null) {
            screenManager.replace(screen);
        }
    }

    public static boolean back() {
        return screenManager != null && screenManager.back();
    }

    public static void clearHistory() {
        if (screenManager != null) {
            screenManager.clearHistory();
        }
    }

    public static Display getDisplay() {
        return screenManager == null ? null : screenManager.getDisplay();
    }

    public static Displayable getCurrent() {
        return screenManager == null ? null : screenManager.getCurrent();
    }
}
