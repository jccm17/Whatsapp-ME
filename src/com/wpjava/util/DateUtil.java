package com.wpjava.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static String shortDate(long time) {
        if (time <= 0) {
            return "";
        }
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(time));
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1;
        return two(day) + "/" + two(month);
    }

    public static String time(long time) {
        if (time <= 0) return "";
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(time));
        return two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE));
    }

    public static boolean sameDay(long first, long second) {
        if (first <= 0 || second <= 0) return false;
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        a.setTime(new Date(first));
        b.setTime(new Date(second));
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }

    public static String conversationDate(long time) {
        if (time <= 0) return "";
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(time));
        return two(c.get(Calendar.DAY_OF_MONTH)) + "/"
                + two(c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
    }

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
