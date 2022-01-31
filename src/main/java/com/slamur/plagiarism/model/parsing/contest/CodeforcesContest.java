package com.slamur.plagiarism.model.parsing.contest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class CodeforcesContest extends Contest {

    private static final String MIREA = "mirea";

    public static final CodeforcesContest MIREA_SCHOOL_QUAL_2022_JAN_30 = new CodeforcesContest(
            MIREA,
            366667,
            LocalDate.of(2022, 1, 30),
            LocalTime.of(12, 0, 0),
            LocalDate.of(2022, 1, 30),
            LocalTime.of(16, 0, 0),
            problemsRange('A', 'G'),
            "/home/slamur/Documents/MIREA/School/Qual 30.01.2022/Solutions"
    );

    private final String localDirectoryPath;

    public CodeforcesContest(String type,
                             int id,
                             LocalDate startDate,
                             LocalTime startTime,
                             LocalDate endDate,
                             LocalTime endTime,
                             List<String> problems,
                             String localDirectoryPath) {
        super(type,
                id,
                LocalDateTime.of(startDate, startTime),
                LocalDateTime.of(endDate, endTime),
                problems,
                true,
                1
        );

        this.localDirectoryPath = localDirectoryPath;
    }

    public String getLocalDirectoryPath() {
        return localDirectoryPath;
    }
}
