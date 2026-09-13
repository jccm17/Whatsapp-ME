package com.wpjava.storage;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

public class RecordStorage {

    protected void replace(String storeName, String value) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(storeName, true);
            byte[] data = value.getBytes("UTF-8");
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Exception e) {
        } finally {
            close(rs);
        }
    }

    protected String readFirst(String storeName) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(storeName, true);
            if (rs.getNumRecords() == 0) {
                return null;
            }
            byte[] data = rs.getRecord(1);
            return new String(data, "UTF-8");
        } catch (Exception e) {
            return null;
        } finally {
            close(rs);
        }
    }

    protected void add(String storeName, String value) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(storeName, true);
            byte[] data = value.getBytes("UTF-8");
            rs.addRecord(data, 0, data.length);
        } catch (Exception e) {
        } finally {
            close(rs);
        }
    }

    protected void replaceAll(String storeName, String[] values) {
        RecordStore rs = null;
        try {
            try {
                RecordStore.deleteRecordStore(storeName);
            } catch (Exception ignored) {
            }
            rs = RecordStore.openRecordStore(storeName, true);
            int i;
            for (i = 0; values != null && i < values.length; i++) {
                byte[] data = values[i].getBytes("UTF-8");
                rs.addRecord(data, 0, data.length);
            }
        } catch (Exception e) {
        } finally {
            close(rs);
        }
    }

    protected String[] readAll(String storeName) {
        RecordStore rs = null;
        RecordEnumeration en = null;
        try {
            rs = RecordStore.openRecordStore(storeName, true);
            String[] values = new String[rs.getNumRecords()];
            en = rs.enumerateRecords(null, null, false);
            int i = 0;
            while (en.hasNextElement()) {
                byte[] data = en.nextRecord();
                values[i++] = new String(data, "UTF-8");
            }
            return values;
        } catch (Exception e) {
            return new String[0];
        } finally {
            if (en != null) {
                en.destroy();
            }
            close(rs);
        }
    }

    protected void clear(String storeName) {
        try {
            RecordStore.deleteRecordStore(storeName);
        } catch (Exception e) {
        }
    }

    protected void close(RecordStore rs) {
        try {
            if (rs != null) {
                rs.closeRecordStore();
            }
        } catch (Exception e) {
        }
    }
}
