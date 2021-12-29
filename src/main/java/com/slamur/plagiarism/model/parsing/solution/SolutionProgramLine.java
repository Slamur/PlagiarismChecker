package com.slamur.plagiarism.model.parsing.solution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class SolutionProgramLine {

    final static String[] KEYWORDS = {
            "for", "while", "do",
            "repeat", "until",

            "if", "then", "else",
            "not", "and", "or",

            "div", "mod",
            "shl", "shr",

            "case", "switch",
            "continue", "break",
            "def", "self",
            "return",
            "new", "delete",
            "begin", "end",

            "void",
            "int", "integer",
            "long",
            "string", "char",
            "float", "double",
            "bool", "boolean",

            "cin", "cout", "scanf", "printf",
            "scanner", "system", "in", "out",
            "input", "print", "println",
            "read", "readln", "write", "writeln",

            "map", "hashmap", "treemap",
            "list", "arraylist", "vector",
            "set", "hashset", "treeset",
            "queue", "deque", "stack",
            "pair",

            "abs", "min", "max",
            "sin", "cos",

            "public", "private", "static",
            "final", "const",
    };

    final static String BRACKETS = "[]{}()<>";
    private final static Set<String> KEYWORDS_SET;

    static {
        KEYWORDS_SET = new HashSet<>();
        KEYWORDS_SET.addAll(Arrays.asList(KEYWORDS));
    }

    static boolean isBracket(char ch) {
        return BRACKETS.indexOf(ch) >= 0;
    }

    static boolean isKeyword(String word) {
        return KEYWORDS_SET.contains(word);
    }

    static boolean isSpecial(char ch) {
        return !Character.isLetterOrDigit(ch);
    }

    static boolean isSpecial(String word) {
        char[] s = word.toCharArray();
        for (char ch : s) {
            if (!isSpecial(ch)) return false;
        }

        return true;
    }

    static boolean isNumber(String word) {
        char[] s = word.toCharArray();

        if (s[0] != '-' && s[0] != '+' && !Character.isDigit(s[0])) {
            return false;
        }

        for (int i = 1; i < s.length; ++i) {
            if (!Character.isDigit(s[i])) {
                return false;
            }
        }

        return true;
    }

    static boolean isWord(String word) {
        return isSpecial(word) || isNumber(word) || isKeyword(word);
    }

    public static SolutionProgramLine parse(String line) {
        char[] s = line.toLowerCase().toCharArray();

        List<String> words = new ArrayList<>();
        List<String> names = new ArrayList<>();

        String word = "";
        for (char ch : s) {
            if (Character.isWhitespace(ch)) {
                if (!word.isEmpty()) {
                    if (isWord(word)) words.add(word);
                    else names.add(word);

                    word = "";
                }
            } else if (isBracket(ch)) {
                if (!word.isEmpty()) {
                    if (isWord(word)) words.add(word);
                    else names.add(word);

                    word = "";
                }

                words.add("" + ch);
            } else if (isSpecial(ch)) {
                if (!isSpecial(word)) {
                    if (isWord(word)) words.add(word);
                    else names.add(word);

                    word = "";
                }

                word += ch;
            } else {
                if (!word.isEmpty() && isSpecial(word)) {
                    words.add(word);
                    word = "";
                }

                word += ch;
            }
        }

        if (!word.isEmpty()) {
            if (isWord(word)) {
                words.add(word);
            } else {
                names.add(word);
            }
        }

        return new SolutionProgramLine(words, names);
    }

    private final List<String> words;
    private final List<String> names;

    private final Map<String, Integer> wordCounts;

    public SolutionProgramLine(List<String> words, List<String> names) {
        this.words = words;
        this.names = names;

        this.wordCounts = new HashMap<>();
        for (String word : words) {
            int count = wordCounts.getOrDefault(word, 0);
            wordCounts.put(word, count + 1);
        }
    }

    public double calculateSimilarity(SolutionProgramLine other) {
        int total = 0;
        int both = 0;

        for (var entry : wordCounts.entrySet()) {
            int thisCount = entry.getValue();
            int otherCount = other.wordCounts.getOrDefault(entry.getKey(), 0);

            both += min(thisCount, otherCount);
            total += max(thisCount, otherCount);
        }

        for (var entry : other.wordCounts.entrySet()) {
            int otherCount = entry.getValue();
            int thisCount = wordCounts.getOrDefault(entry.getKey(), 0);

            if (thisCount == 0) {
                total += otherCount;
            }
        }

        return (double)both / Math.max(1, total);
    }
}
