package com.slamur.plagiarism.model.parsing.solution;

import java.util.Collections;
import java.util.List;

public enum Verdict {
    AC(List.of("OK", "AC", "Accepted")),
    WA(List.of("WA", "Wrong answer")),
    TL(List.of("TL", "Time-limit exceeded")),
    ML(List.of("ML", "Memory limit exceeded")),
    RE(List.of("RE", "RT", "Run-time error")),
    CE(List.of("CE", "Compilation error")),
    PE(List.of("PE", "Presentation error")),
    DQ(List.of("DQ", "Disqualified")),
    WT(List.of("WT", "Wall TLE")),
    VIRTUAL_START(List.of("VS", "Virtual start")),
    VIRTUAL_FINISH(List.of("VT", "Virtual stop")),
    DISQUALIFIED(Collections.emptyList()),
    UNKNOWN(Collections.emptyList());

    List<String> aliases;

    Verdict(List<String> aliases) {
        this.aliases = aliases;
    }

    public static Verdict fromText(String verdictText) {
        for (Verdict verdict : values()) {
            for (String alias : verdict.aliases) {
                if (verdictText.contains(alias)) return verdict;
            }
        }

        return UNKNOWN;
    }
}