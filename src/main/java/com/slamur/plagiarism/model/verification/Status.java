package com.slamur.plagiarism.model.verification;

public enum Status {

    NOT_SEEN("Не просмотрено", 0),
    IGNORED("Не скопировано", 1),
    UNKNOWN("Спорно", 2),
    AUTOPLAGIAT("Автоплагиат", 3),
    PLAGIAT("Скопировано", 4);

    public final String text;
    public final int priority;

    Status(String text, int priority) {
        this.text = text;
        this.priority = priority;
    }
}
