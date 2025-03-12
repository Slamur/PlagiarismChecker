package com.slamur.plagiarism.model.parsing.contest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

public class DirectoryContest extends Contest {

    private static final String NLOGN = "NlogN";
    private static final String AUCA = "AUCA";
    private static final String SAMSU = "SAMSU";
    private static final LocalDate NO_DATE = LocalDate.of(1, 1, 1);
    private static final LocalTime NO_TIME = LocalTime.of(1, 1, 1);

    public static final DirectoryContest NLOGN_QUAL_24 = new DirectoryContest(NLOGN, "sps_entrance");
    public static final DirectoryContest DS_PART_2_EXAM_24_25 = new DirectoryContest(AUCA, "ds-part-2");
    public static final DirectoryContest ALGO_PART_2_EXAM_24_25 = new DirectoryContest(AUCA, "algo-part-2");
    public static final DirectoryContest SAMSU_EXAM_24_25 = new DirectoryContest(SAMSU, "samsu-2024-2025",
            IntStream.range(0, 26).mapToObj(
                index -> String.valueOf((char)(index + 'A'))
            ).toList()
    );

    private final String directory;

    public DirectoryContest(String type, String directory) {
        this(type, directory, List.of("A"));
    }

    public DirectoryContest(String type, String directory, List<String> problems) {
        super(type, -1, NO_DATE, NO_TIME, NO_TIME, problems, false, 1);
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }
}
