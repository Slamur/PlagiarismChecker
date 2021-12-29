package com.slamur.plagiarism.service.impl.contest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.slamur.plagiarism.utils.IOUtils;

public class StandingsRepository {

    private static final String STANDINGS_FILE_NAME = "standings.csv";

    private final File directory;

    public StandingsRepository(File directory) {
        this.directory = directory;
    }

    public void saveStandings(Consumer<PrintWriter> writeStrategy)
            throws FileNotFoundException, UnsupportedEncodingException {
        var standingsFile = new File(directory, STANDINGS_FILE_NAME);
        IOUtils.saveToFile(standingsFile, writeStrategy, StandardCharsets.UTF_8.name());
    }
}
