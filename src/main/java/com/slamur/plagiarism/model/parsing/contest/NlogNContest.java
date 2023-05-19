package com.slamur.plagiarism.model.parsing.contest;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.slamur.plagiarism.utils.IOUtils;

public class NlogNContest extends Contest {

    private static final String LSHP = "lshp";

    public static final NlogNContest MIREA_SCHOOL_QUAL_2022_JAN_30 = NlogNContest.create(
            LSHP,
            3965,
            LocalDate.of(2023, 5, 1),
            LocalTime.of(0, 0, 0),
            LocalDate.of(2023, 5, 15),
            LocalTime.of(23, 59, 59),
            "/home/slamur/Documents/Plagiarism/NlogN/Qual 2023/tasks"
    );

    static NlogNContest create(
            String type,
            int id,
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime,
            String localDirectoryPath
    ) {
        File localDirectory = new File(localDirectoryPath);
        if (!localDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("Путь не соответствует директории: %s", localDirectoryPath)
            );
        }

        List<String> problems = IOUtils.getFiles(localDirectory).stream()
                .filter(File::isDirectory)
                .map(File::getName)
                .toList();

        return new NlogNContest(
                type,
                id,
                startDate,
                startTime,
                endDate,
                endTime,
                problems,
                localDirectory
        );
    }

    private final File localDirectory;

    public NlogNContest(String type,
                        int id,
                        LocalDate startDate,
                        LocalTime startTime,
                        LocalDate endDate,
                        LocalTime endTime,
                        List<String> problems,
                        File localDirectory) {
        super(type,
                id,
                LocalDateTime.of(startDate, startTime),
                LocalDateTime.of(endDate, endTime),
                problems,
                true,
                1
        );

        this.localDirectory = localDirectory;
    }

    public File getLocalDirectory() {
        return localDirectory;
    }
}
