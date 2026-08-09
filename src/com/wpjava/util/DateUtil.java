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

    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
