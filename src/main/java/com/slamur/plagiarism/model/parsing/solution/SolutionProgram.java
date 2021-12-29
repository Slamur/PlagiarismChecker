package com.slamur.plagiarism.model.parsing.solution;

import java.util.ArrayList;
import java.util.List;

import com.slamur.plagiarism.logic.MatchingSimilarityCalculator;

public class SolutionProgram {

    public static double calculateSimilarity(SolutionProgram leftSolution,
                                             SolutionProgram rightSolution,
                                             double filter,
                                             double minimalSimilarityLimit
                                             ) {
        List<SolutionProgramLine> leftRows = leftSolution.parsedLines;
        List<SolutionProgramLine> rightRows = rightSolution.parsedLines;

        if (leftRows.isEmpty() || rightRows.isEmpty()) {
            return 0;
        }

        return MatchingSimilarityCalculator.calculate(leftRows, rightRows, filter, minimalSimilarityLimit);
    }

    public final Language language;
    public final String code;
    private final List<SolutionProgramLine> parsedLines;

    public SolutionProgram(Language language, String code) {
        this.language = language;
        this.code = code;

        this.parsedLines = new ArrayList<>();
        parseCode();
    }

    private void parseCode() {
        String parsedCode = code;

        final char single = 1, multiStart = 2, multiEnd = 3, newLine = '\n', multiEqual = 4;

        if (!language.singleLineCommentStart.isEmpty()) {
            parsedCode = parsedCode.replaceAll(language.singleLineCommentStart, "" + single);
        }

        for (var e : language.multilineComments.entrySet()) {
            if (e.getKey().equals(e.getValue())) {
                parsedCode = parsedCode.replaceAll(e.getKey(), "" + multiEqual);
            } else {
                parsedCode = parsedCode.replaceAll(e.getKey(), "" + multiStart);
                parsedCode = parsedCode.replaceAll(e.getValue(), "" + multiEnd);
            }
        }

        parsedLines.clear();

        StringBuilder parsedCodeBuilder = new StringBuilder();
        for (int i = 0; i < parsedCode.length(); ++i) {
            char ch = parsedCode.charAt(i);
            if (single == ch) {
                i = parsedCode.indexOf(newLine, i + 1) - 1;
                continue;
            }

            if (multiStart == ch) {
                i = parsedCode.indexOf(multiEnd, i + 1);
                continue;
            }

            if (multiEqual == ch) {
                i = parsedCode.indexOf(multiEqual, i + 1);
                continue;
            }

            parsedCodeBuilder.append(ch);
        }

//        while (true) {
//            int multiStartIndex = parsedCode.indexOf(multiStart);
//            if (multiStartIndex < 0) break;
//
//            int multiEndIndex = parsedCode.indexOf(multiEnd, multiStartIndex);
//            parsedCode = parsedCode.substring(0, multiStartIndex) + parsedCode.substring(multiEndIndex + 1);
//        }
//
//        while (true) {
//            int singleIndex = parsedCode.indexOf(single);
//            if (singleIndex < 0) break;
//
//            int newLineIndex = parsedCode.indexOf(newLine, singleIndex);
//            parsedCode = parsedCode.substring(0, singleIndex) + parsedCode.substring(newLineIndex);
//        }

        String[] codeLines = parsedCodeBuilder.toString().split("" + newLine);
        for (var line : codeLines) {
            if (!line.trim().isEmpty()) {
                parsedLines.add(
                        SolutionProgramLine.parse(line)
                );
            }
        }
    }

    @Override
    public String toString() {
        var codeLines = (code.isEmpty() ? 0 : code.split("\n").length);
        return language + "\n" + codeLines + "\n" + code;
    }
}
