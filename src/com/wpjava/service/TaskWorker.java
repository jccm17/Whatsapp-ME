package com.wpjava.service;

import java.util.Vector;

/** Single FIFO worker for serial background work on constrained MIDP devices. */
public class TaskWorker implements Runnable {

    private final Vector queue = new Vector();
    private final Object pauseLock = new Object();
    private Thread thread;
    private boolean running;
    private boolean paused;

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        synchronized (queue) { queue.notifyAll(); }
        synchronized (pauseLock) { pauseLock.notifyAll(); }
    }

    public void submit(Runnable task) {
        if (task == null) return;
        synchronized (queue) {
            queue.addElement(task);
            queue.notifyAll();
        }
    }

    public void pause() {
        synchronized (pauseLock) { paused = true; }
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public void run() {
        while (isRunning()) {
            Runnable task = nextTask();
            if (task == null) continue;
            waitWhilePaused();
            if (!isRunning()) break;
            try {
                task.run();
            } catch (Throwable ignored) {
                // A failed task must not stop pending sync or upload work.
            }
        }
    }

    private Runnable nextTask() {
        synchronized (queue) {
            while (queue.isEmpty() && isRunning()) {
                try { queue.wait(); } catch (InterruptedException ignored) { }
            }
            if (!isRunning() || queue.isEmpty()) return null;
            Runnable task = (Runnable) queue.elementAt(0);
            queue.removeElementAt(0);
            return task;
        }
    }

    private void waitWhilePaused() {
        synchronized (pauseLock) {
            while (paused && isRunning()) {
                try { pauseLock.wait(); } catch (InterruptedException ignored) { }
            }
        }
    }
}
