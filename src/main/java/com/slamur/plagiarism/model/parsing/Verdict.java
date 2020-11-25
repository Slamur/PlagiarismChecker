package com.slamur.plagiarism.model.parsing;

public enum Verdict {
    AC, WA, CE, UNKNOWN;

    public static Verdict fromText(String verdictText) {
        if (verdictText.contains("Compilation error")) return CE;
        if (verdictText.contains("Wrong answer")) return WA;
        if (verdictText.contains("Accepted")) return AC;
        return UNKNOWN;
    }
}