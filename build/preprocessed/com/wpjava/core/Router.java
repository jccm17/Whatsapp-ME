package com.wpjava.core;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

public class Router {

    private static Display display;

    private Router() {
    }

    public static void init(Display d) {
        display = d;
    }

    public static void navigate(Displayable screen) {
        if (display != null) {
            display.setCurrent(screen);
        }
    }

    public static Display getDisplay() {
        return display;
    }
}
