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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RecursiveDirectoryContestLoader implements ContestLoader {

    public static RecursiveDirectoryContestLoader forContest(DirectoryContest contest) {
        String directoryPath = contest.getDirectory();
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("Путь не соответствует директории: %s", directoryPath)
            );
        }

        var participantsSolutionsInfo = readParticipantsSolutionsInfo(directory);

        return new RecursiveDirectoryContestLoader(
                contest,
                participantsSolutionsInfo
        );
    }

    private record SolutionInfo(String problemId, String id, String languageAlias, File file) { }

    private static Map<ParticipantInfo, List<SolutionInfo>> readParticipantsSolutionsInfo(
            File directory
    ) {
        return IOUtils.getFiles(directory).stream().collect(
                Collectors.toMap(
                        participantDirectory -> new ParticipantInfo(
                                participantDirectory.getName(),
                                ParticipantInfo.NOT_AVAILABLE,
                                ParticipantInfo.NOT_AVAILABLE
                        ),
                        participantDirectory -> IOUtils.getFiles(participantDirectory).stream().flatMap(problemDirectory -> {
                               String problemId = problemDirectory.getName();
                               return IOUtils.getFiles(problemDirectory).stream().map(solutionFile -> {
                                    String name = solutionFile.getName();

                                    int dotIndex = name.indexOf(".");
                                    String id = name.substring(0, dotIndex);
                                    String languageAlias = name.substring(dotIndex + 1);
                                    return new SolutionInfo(problemId, id, languageAlias, solutionFile);
                               });
                        }).toList()
                )
        );
    }

    private final DirectoryContest contest;
    private final Map<Participant, List<SolutionInfo>> participantSolutions;

    public RecursiveDirectoryContestLoader(DirectoryContest contest, Map<ParticipantInfo, List<SolutionInfo>> participantsSolutionsInfo) {
        this.contest = contest;

        this.participantSolutions = participantsSolutionsInfo.entrySet().stream().collect(
                Collectors.toMap(
                        entry -> new Participant(entry.getKey().name, contest),
                        Map.Entry::getValue
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
        return new ParticipantInfo(participant.id, ParticipantInfo.NOT_AVAILABLE, ParticipantInfo.NOT_AVAILABLE);
    }

    @Override
    public ParticipantSolutions loadSolutions(Participant participant) {
        var solutionInfos = participantSolutions.get(participant);

        var solutions = new ParticipantSolutions();

        solutionInfos.forEach(solutionInfo -> {
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
                        solutionInfo.problemId,
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
        });

        return solutions;
    }
}
