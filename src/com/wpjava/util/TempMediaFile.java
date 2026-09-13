package com.wpjava.util;

import java.io.OutputStream;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/** Creates media files on an available JSR-75 storage root (Nokia C:/, E:/, etc.). */
public final class TempMediaFile {

    private TempMediaFile() { }

    public static String prepare(String name) throws Exception {
        String path = tryRoot("E:/", name);
        if (path != null) return path;
        path = tryRoot("C:/", name);
        if (path != null) return path;
        Enumeration roots = FileSystemRegistry.listRoots();
        while (roots.hasMoreElements()) {
            path = tryRoot((String) roots.nextElement(), name);
            if (path != null) return path;
        }
        throw new Exception("No hay unidad de almacenamiento disponible");
    }

    public static String write(String name, byte[] data) throws Exception {
        String path = prepare(name);
        FileConnection file = null;
        OutputStream output = null;
        try {
            file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (file.exists()) file.delete();
            file.create();
            output = file.openOutputStream();
            output.write(data);
            output.flush();
            return path;
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) { }
            try { if (file != null) file.close(); } catch (Exception ignored) { }
        }
    }

    private static String tryRoot(String root, String name) {
        FileConnection directory = null;
        FileConnection file = null;
        try {
            String base = "file:///" + root + "wpjava/";
            directory = (FileConnection) Connector.open(base, Connector.READ_WRITE);
            if (!directory.exists()) directory.mkdir();
            directory.close();
            directory = null;
            String path = base + name;
            file = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (file.exists()) file.delete();
            file.create();
            return path;
        } catch (Exception ignored) {
            return null;
        } finally {
            try { if (file != null) file.close(); } catch (Exception ignored) { }
            try { if (directory != null) directory.close(); } catch (Exception ignored) { }
        }
    }
}
