package com.slamur.plagiarism.utils;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    public static final String DATE_TIME_PATTERN = "HH:mm:ss dd.MM.yyyy";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    public static long toCeilingMinutes(Duration duration) {
        long minutes = duration.toMinutes();
        if (duration.toSecondsPart() > 0) ++minutes;
        return minutes;
    }
}
