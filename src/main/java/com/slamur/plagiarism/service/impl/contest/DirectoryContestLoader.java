package com.slamur.plagiarism.service.impl.contest;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.contest.DirectoryContest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.participant.ParticipantSolutions;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.SolutionProgram;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DirectoryContestLoader implements ContestLoader {

    public static DirectoryContestLoader forContest(DirectoryContest contest) {
        String directoryPath = contest.getDirectory();
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("Путь не соответствует директории: %s", directoryPath)
            );
        }

        var solutionInfos = readSolutionInfos(directory);

        return new DirectoryContestLoader(
                contest,
                solutionInfos
        );
    }

    private record SolutionInfo(String id, String languageAlias, File file) { }

    private static List<SolutionInfo> readSolutionInfos(
            File directory
    ) {
        return IOUtils.getFiles(directory).stream().map(file -> {
            String name = file.getName();
            
            int dotIndex = name.indexOf(".");
            String id = name.substring(0, dotIndex);
            String languageAlias = name.substring(dotIndex + 1);
            return new SolutionInfo(id, languageAlias, file);

        }).toList();
    }

    private final DirectoryContest contest;
    private final Map<Participant, SolutionInfo> participantSolutions;

    public DirectoryContestLoader(DirectoryContest contest, List<SolutionInfo> solutionInfos) {
        this.contest = contest;

        this.participantSolutions = solutionInfos.stream().collect(
                Collectors.toMap(
                        info -> new Participant(info.id, contest),
                        Function.identity()
                )
        );
    }

    @Override
    public Contest getContest() {
        return contest;
    }

    @Override
    public Set<Participant> loadParticipants(Contest contest) {
        return Set.copyOf(participantSolutions.keySet());
    }

    @Override
    public ParticipantInfo loadParticipantInfo(Participant participant) {
        return ParticipantInfo.INFO_NOT_AVAILABLE;
    }

    @Override
    public ParticipantSolutions loadSolutions(Participant participant) {
        var solutionInfo = participantSolutions.get(participant);

        var solutions = new ParticipantSolutions();

        try {
            var solutionProgram = IOUtils.loadFromFile(solutionInfo.file(), in -> {
                var lines = in.lines().toList();
                return SolutionProgram.create(
                        solutionInfo.languageAlias,
                        String.join("\n", lines),
                        Verdict.AC
                );
            });

            var solution = new Solution(
                    solutionInfo.id(),
                    participant,
                    "A",
                    solutionProgram,
                    Verdict.AC,
                    1,
                    LocalDateTime.now(),
                    solutionInfo.id()
            );

            solutions.addSolution(solution);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return solutions;
    }
}
