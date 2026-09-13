package com.wpjava.core;

import java.util.Stack;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import com.wpjava.WPJavaMidlet;

/** Maintains the MIDlet screen stack and forwards optional lifecycle events. */
public class ScreenManager {

    private WPJavaMidlet midlet;
    private Display display;
    private Stack history;
    private Displayable current;

    public ScreenManager(WPJavaMidlet midlet) {
        this.midlet = midlet;
        display = Display.getDisplay(midlet);
        history = new Stack();
    }

    public void show(Displayable screen) {
        if (current != null) {
            pause(current);
            history.push(current);
        }
        setCurrent(screen);
    }

    public void replace(Displayable screen) {
        if (current != null) {
            pause(current);
        }
        setCurrent(screen);
    }

    public boolean back() {
        if (history.empty()) {
            return false;
        }
        if (current != null) {
            pause(current);
        }
        setCurrent((Displayable) history.pop());
        return true;
    }

    public void clearHistory() {
        history.removeAllElements();
    }

    public Displayable getCurrent() {
        return current;
    }

    public Display getDisplay() {
        return display;
    }

    public WPJavaMidlet getMidlet() {
        return midlet;
    }

    private void setCurrent(Displayable screen) {
        current = screen;
        display.setCurrent(screen);
        if (screen instanceof ScreenLifecycle) {
            ((ScreenLifecycle) screen).onResume();
        }
    }

    private void pause(Displayable screen) {
        if (screen instanceof ScreenLifecycle) {
            ((ScreenLifecycle) screen).onPause();
        }
    }

    public interface ScreenLifecycle {
        void onResume();
        void onPause();
    }
}
