package com.slamur.plagiarism.model.verification;

import java.util.Arrays;

public enum Status {

    NOT_SEEN("Не просмотрено", false),
    PLAGIAT("Скопировано", false),
    UNKNOWN("Спорно", true),
    IGNORED("Не скопировано", true);

    public final String text;
    public final boolean isEdge;

    Status(String text, boolean isEdge) {
        this.text = text;
        this.isEdge = isEdge;
    }

    public static Status byText(String text) {
        return Arrays.stream(values())
                .filter(status -> status.text.equals(text))
                .findFirst()
                .orElse(NOT_SEEN);
    }
}
