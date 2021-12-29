package com.slamur.plagiarism.model.parsing.contest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.solution.Solution;

public class Contest {

    private static final int ICPC_PENALTY_TIME = 20;

    public static List<String> problemsRange(char startProblem, char endProblem) {
        List<String> problems = new ArrayList<>();

        for (char problemChar = startProblem; problemChar <= endProblem; ++problemChar) {
            problems.add(String.valueOf(problemChar));
        }

        return problems;
    }

    private final String type;
    private final int id;
    private final LocalDateTime startDateTime, endDateTime;

    private final List<String> problems;

    private final boolean compareOnlyBestSolutions;
    private final int minimalProblemsToCompare;

    public Contest(String type,
                   int id,
                   LocalDate date,
                   LocalTime startTime,
                   LocalTime endTime,
                   List<String> problems,
                   boolean compareOnlyBestSolutions,
                   int minimalProblemsToCompare) {
        this(type,
                id,
                LocalDateTime.of(date, startTime),
                LocalDateTime.of(date, endTime),
                problems,
                compareOnlyBestSolutions,
                minimalProblemsToCompare
        );
    }

    public Contest(String type,
                   int id,
                   LocalDateTime startDateTime,
                   LocalDateTime endDateTime,
                   List<String> problems,
                   boolean compareOnlyBestSolutions,
                   int minimalProblemsToCompare) {
        this.type = type;
        this.id = id;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.problems = Collections.unmodifiableList(problems);
        this.compareOnlyBestSolutions = compareOnlyBestSolutions;
        this.minimalProblemsToCompare = minimalProblemsToCompare;
    }

    public String getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public List<String> getProblems() {
        return problems;
    }

    public String toText(Participant participant) {
        return participant.toString();
    }

    public String toText(Solution solution) {
        return solution.getId();
    }

    public boolean compareOnlyBestSolutions() {
        return compareOnlyBestSolutions;
    }

    public int getMinimalProblemsToCompare() {
        return minimalProblemsToCompare;
    }

    public List<String> getInterestingProblems() {
        return getProblems();
    }

    public int getPenaltyTime() {
        return ICPC_PENALTY_TIME;
    }
}
