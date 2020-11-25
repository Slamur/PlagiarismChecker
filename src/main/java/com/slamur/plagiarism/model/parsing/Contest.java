package com.slamur.plagiarism.model.parsing;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.slamur.plagiarism.utils.AlertUtils;

public class Contest {

    private static final File contestsFolder;

    static {
        contestsFolder = new File("contests");
        if (!contestsFolder.exists()) {
            if (!contestsFolder.mkdir()) {
                AlertUtils.error("Проблема при создании папки для хранения контестов");
            }
        }
    }

    public static final String SCHOOL = "sch", CITY = "okrug";

    private final String type;
    private final int id;
    private final LocalDateTime startDateTime, endDateTime;

    private final int problemsCount;

    public Contest(String type, int id,
                   LocalDate date,
                   LocalTime startTime, LocalTime endTime,
                   int problemsCount) {
        this.type = type;
        this.id = id;
        this.startDateTime = LocalDateTime.of(date, startTime);
        this.endDateTime = LocalDateTime.of(date, endTime);
        this.problemsCount = problemsCount;
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

    public int getProblemsCount() {
        return problemsCount;
    }

    public File createFolder() throws IOException {

        var folderName = getType() + getId();

        var folder = new File(contestsFolder, folderName);
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                throw new IOException("Проблема при создании папки контеста");
            }
        }

        return folder;
    }
}
