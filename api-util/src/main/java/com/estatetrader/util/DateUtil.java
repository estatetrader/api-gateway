package com.estatetrader.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class DateUtil {
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^[\\d]+$");

    public static Date toDate(String dateStr) {
        if (TIMESTAMP_PATTERN.matcher(dateStr).matches()) {
            return new Date(Long.parseLong(dateStr));
        }

        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(dateStr);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toString(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(date);
    }
}
