package com.slamur.plagiarism.model.verification;

import java.util.Arrays;

public enum Status {

    NOT_SEEN("Не просмотрено", 0),
    IGNORED("Не скопировано", 1),
    UNKNOWN("Спорно", 2),
    PLAGIAT("Скопировано", 3);

    public final String text;
    public final int priority;

    Status(String text, int priority) {
        this.text = text;
        this.priority = priority;
    }

    public static Status byText(String text) {
        return Arrays.stream(values())
                .filter(status -> status.text.equals(text))
                .findFirst()
                .orElse(NOT_SEEN);
    }
}
