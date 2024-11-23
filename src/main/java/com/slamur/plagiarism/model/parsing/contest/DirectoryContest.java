package com.slamur.plagiarism.model.parsing.contest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DirectoryContest extends Contest {

    private static final String NLOGN = "NlogN";
    private static final LocalDate NO_DATE = LocalDate.of(1, 1, 1);
    private static final LocalTime NO_TIME = LocalTime.of(1, 1, 1);

    public static final DirectoryContest NLOGN_QUAL_24 = new DirectoryContest(NLOGN, "sps_entrance");

    private final String directory;

    public DirectoryContest(String type, String directory) {
        super(type, -1, NO_DATE, NO_TIME, NO_TIME, List.of("A"), false, 1);
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }
}
