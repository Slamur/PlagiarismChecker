package com.slamur.plagiarism.model.parsing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.slamur.plagiarism.logic.MatchingSimilarityCalculator;
import com.slamur.plagiarism.utils.RequestUtils;

public class Solution {

    public static final String DATE_TIME_PATTERN = "HH:mm:ss dd.MM.yyyy";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    public static double calculateSimilarity(Solution leftSolution, Solution rightSolution, double filter) {
        if (null == leftSolution.rows) {
            leftSolution.build();
        }

        if (null == rightSolution.rows) {
            rightSolution.build();
        }

        List<SolutionRow> leftRows = leftSolution.rows;
        List<SolutionRow> rightRows = rightSolution.rows;

        if (leftRows.isEmpty() || rightRows.isEmpty()) {
            return 0;
        }
        
        int leftSize = leftRows.size();
        int rightSize = rightRows.size();

        double[][] sims = new double[leftSize][rightSize];
        for (int i = 0; i < leftSize; ++i) {
            for (int j = 0; j < rightSize; ++j) {
                sims[i][j] = leftRows.get(i).calculateSimilarity(rightRows.get(j));
            }
        }
        
        return new MatchingSimilarityCalculator(sims).calculate(filter);
    }

    public final String link;
    public final String code;
    public final Verdict verdict;
    public final int score;
    public final LocalDateTime dateTime;

    private List<SolutionRow> rows;

    public Solution(String link, String code, Verdict verdict, int score, LocalDateTime dateTime) {
        this.link = link;
        this.code = code;
        this.verdict = verdict;
        this.score = score;
        this.dateTime = dateTime;
    }

    public void build() {
        this.rows = new ArrayList<>();
        for (var line : code.split("\n")) {
            if (!line.trim().isEmpty()) {
                rows.add(SolutionRow.parse(line));
            }
        }
    }

    @Override
    public String toString() {
        return getFullLink() + "\t" + getDateTimeString();
    }

    public String toText() {
        int codeSize = code.split("\n").length;
        return link + "\n" +
                verdict + "\n" +
                score + "\n" +
                getDateTimeString() + "\n" +
                codeSize + "\n" +
                code + "\n";
    }

    public String getFullLink() {
        return RequestUtils.DOMAIN + link;
    }

    public String getDateTimeString() {
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
