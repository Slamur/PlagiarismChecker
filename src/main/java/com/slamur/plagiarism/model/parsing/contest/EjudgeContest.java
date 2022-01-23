package com.slamur.plagiarism.model.parsing.contest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class EjudgeContest extends Contest {

    private static final String KOTECH = "kotech";

    public static final EjudgeContest KOTECH_2021_QUAL = new EjudgeContest(
            KOTECH,
            220,
            LocalDate.of(2021, 12, 10),
            LocalTime.of(0, 0, 0),
            LocalDate.of(2021, 12, 20),
            LocalTime.of(4, 0, 0),
            problemsRange('A', 'H'),
            "/home/slamur/Documents/Cognitive Technologies/2021-2022/Qual/Solutions/contest_220_20211223215014",
            "/home/slamur/Documents/Cognitive Technologies/2021-2022/Qual/Solutions/solutions.csv",
            1
    ) {
        @Override
        public List<String> getInterestingProblems() {
            return EjudgeContest.problemsRange('C', 'H');
        }
    };

    public static final EjudgeContest KOTECH_2021_FINALS = new EjudgeContest(
            KOTECH,
            242,
            LocalDate.of(2022, 1, 23),
            LocalTime.of(10, 0, 0),
            LocalDate.of(2022, 1, 23),
            LocalTime.of(15, 0, 0),
            problemsRange('A', 'I'),
            // TODO fix later
            "/home/slamur/Documents/Cognitive Technologies/2021-2022/Qual/Solutions/contest_220_20211223215014",
            "/home/slamur/Documents/Cognitive Technologies/2021-2022/Qual/Solutions/solutions.csv",
            1
    ) {
        @Override
        public List<String> getInterestingProblems() {
            return EjudgeContest.problemsRange('C', 'H');
        }
    };

    private final String localDirectoryPath;
    private final String solutionsInfoCsvPath;

    public EjudgeContest(String type,
                         int id,
                         LocalDate startDate,
                         LocalTime startTime,
                         LocalDate endDate,
                         LocalTime endTime,
                         List<String> problems,
                         String localDirectoryPath,
                         String solutionsInfoCsvPath,
                         int minimalProblemsToCompare) {
        super(type,
                id,
                LocalDateTime.of(startDate, startTime),
                LocalDateTime.of(endDate, endTime),
                problems,
                true,
                minimalProblemsToCompare
        );

        this.localDirectoryPath = localDirectoryPath;
        this.solutionsInfoCsvPath = solutionsInfoCsvPath;
    }

    public String getLocalDirectoryPath() {
        return localDirectoryPath;
    }

    public String getSolutionsInfoCsvPath() {
        return solutionsInfoCsvPath;
    }
}
