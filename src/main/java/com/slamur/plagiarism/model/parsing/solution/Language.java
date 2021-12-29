package com.slamur.plagiarism.model.parsing.solution;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public enum Language {

    PASCAL("pas", List.of("fpc"), Map.of("\\{", "\\}"), "//"),
    CPP("cpp", List.of("g++"), Map.of("/\\*", "\\*/"), "//"),
    JAVA("java", List.of("javac"), Map.of("/\\*", "\\*/"), "//"),
    PYTHON("py", List.of("python3"), Map.of("'''", "'''"), "#"),
    TEXT("txt", emptyList(), emptyMap(), "");

    private final String fileExtension;
    private final List<String> aliases;
    public final Map<String, String> multilineComments;
    public final String singleLineCommentStart;

    Language(String fileExtension,
             List<String> aliases,
             Map<String, String> multilineComments,
             String singleLineCommentStart) {
        this.fileExtension = fileExtension;
        this.aliases = aliases;
        this.multilineComments = multilineComments;
        this.singleLineCommentStart = singleLineCommentStart;
    }

    @Override
    public String toString() {
        return fileExtension;
    }

    public static Language fromExtension(String fileExtension) {
        return Arrays.stream(Language.values())
                .filter(language -> fileExtension.equals(language.fileExtension))
                .findAny()
                .orElse(Language.TEXT);
    }

    public static Language fromAlias(String alias) {
        return Arrays.stream(Language.values())
                .filter(language -> language.aliases.contains(alias))
                .findAny()
                .orElse(Language.TEXT);
    }
}
