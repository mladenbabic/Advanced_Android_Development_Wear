package com.example.mladen.wear.util;

import android.content.Context;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * Created by mladen on 3/23/16.
 */
public class DateUtil {

    public static String getCurrentMonthDayAsString(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String dayText;
        if (day < 10) {
            dayText = "0"+ day;
        } else {
            dayText = String.valueOf(day);
        }
        return dayText;
    }

    public static int getFormattedCurrentHours(Context context, Calendar calendar) {
        int h = calendar.get(android.text.format.DateFormat.is24HourFormat(context) ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!android.text.format.DateFormat.is24HourFormat(context) && h == 0) {
            h = 12;
        }
        return h;
    }

    public static int get24CurrentHours(Calendar calendar) {
        int h = calendar.get(Calendar.HOUR_OF_DAY);
        return h;
    }

    public static String getHourFormat(Context context, Calendar calendar) {
        if (!android.text.format.DateFormat.is24HourFormat(context)) {
            String amPm = calendar.get(Calendar.AM_PM) == 0 ? "AM" : "PM";
            return amPm;
        }
        return "";
    }

    public static  int getCurrentHours(Calendar calendar) {
        return calendar.get(Calendar.HOUR);
    }

    public static int getCurrent24Hours(Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int getCurrentMinutes(Calendar calendar) {
       return calendar.get(Calendar.MINUTE);
    }

    public static int getCurrentSeconds(Calendar calendar) {
        return calendar.get(Calendar.SECOND);
    }


    public static String formatTwoDigitNumber(int value) {
        return String.format("%02d", value);
    }


}
