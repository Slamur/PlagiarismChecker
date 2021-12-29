package com.slamur.plagiarism.service.impl.contest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.participant.Participant;

import static com.slamur.plagiarism.utils.IOUtils.getFiles;

public class ContestParticipantsRepository {

    private final Contest contest;
    private final File directory;

    public ContestParticipantsRepository(Contest contest, File directory) {
        this.contest = contest;
        this.directory = directory;
    }

    public List<Participant> getCachedParticipants() {
        return getFiles(directory).stream()
                .filter(File::isDirectory)
                .map(File::getName)
                .map(login -> new Participant(login, contest))
                .collect(Collectors.toList());
    }

    public ParticipantRepository createParticipantRepository(Participant participant) throws IOException {
        File participantDirectory = new File(directory, participant.login);

        if (!participantDirectory.exists()) {
            if (!participantDirectory.mkdir()) {
                throw new IOException(String.format(
                        "Ошибка при создании папки участника %s",
                        participant.login
                ));
            }
        }

        return new ParticipantRepository(participant, participantDirectory);
    }
}