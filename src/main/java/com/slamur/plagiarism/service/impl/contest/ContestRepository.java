package com.slamur.plagiarism.service.impl.contest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.service.impl.comparison.ComparisonRepository;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.IOUtils;

public class ContestRepository {

    private static final File allContestsDirectory;

    static {
        allContestsDirectory = new File("contests");
        if (!allContestsDirectory.exists()) {
            if (!allContestsDirectory.mkdir()) {
                AlertUtils.error("Проблема при создании папки для хранения контестов");
            }
        }
    }

    public static ContestRepository createRepository(Contest contest) throws IOException {

        var directoryName = contest.getType() + contest.getId();

        var directory = new File(allContestsDirectory, directoryName);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Проблема при создании папки контеста");
            }
        }

        return new ContestRepository(contest, directory);
    }

    private final Contest contest;
    private final File directory;

    public ContestRepository(Contest contest, File directory) {
        this.contest = contest;
        this.directory = directory;
    }

    public File getDirectory() {
        return directory;
    }

    public ContestParticipantsRepository createParticipantsRepository() throws IOException {
        File allParticipantsDirectory = new File(directory, "participants");
        if (!allParticipantsDirectory.exists()) {
            if (!allParticipantsDirectory.mkdir()) {
                throw new IOException("Ошибка при создании папки с участниками");
            }
        }

        return new ContestParticipantsRepository(contest, allParticipantsDirectory);
    }

    public ComparisonRepository createComparisonRepository() throws IOException {
        File comparisonsDirectory = new File(directory, "comparisons");
        if (!comparisonsDirectory.exists()) {
            if (!comparisonsDirectory.mkdir()) {
                throw new IOException("Ошибка при создании папки со сравнениями");
            }
        }

        return new ComparisonRepository(comparisonsDirectory);
    }

    public StandingsRepository createStandingsRepository() throws IOException {
        File standingsDirectory = new File(directory, "standings");
        if (!standingsDirectory.exists()) {
            if (!standingsDirectory.mkdir()) {
                throw new IOException("Ошибка при создании папки с положением участников");
            }
        }

        return new StandingsRepository(standingsDirectory);
    }

    public void saveParticipants(List<String> sortedLogins) throws FileNotFoundException, UnsupportedEncodingException {
        File participantLoginsFile = new File(directory, "logins.txt");
        IOUtils.saveToFile(participantLoginsFile, (out) -> sortedLogins.forEach(out::println));
    }
}
