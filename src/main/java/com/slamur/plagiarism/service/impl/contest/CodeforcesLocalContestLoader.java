package com.slamur.plagiarism.service.impl.contest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.parsing.contest.CodeforcesContest;
import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.contest.EjudgeContest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.participant.ParticipantSolutions;
import com.slamur.plagiarism.model.parsing.solution.Language;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.SolutionProgram;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.utils.IOUtils;

public class CodeforcesLocalContestLoader implements ContestLoader {

    public static CodeforcesLocalContestLoader forLocal(CodeforcesContest codeforcesContest) {
        String localDirectoryPath = codeforcesContest.getLocalDirectoryPath();
        File localDirectory = new File(localDirectoryPath);
        if (!localDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("Путь не соответствует директории: %s", localDirectoryPath)
            );
        }

        var solutionsInfo = readSolutionsInfo(codeforcesContest);

        return new CodeforcesLocalContestLoader(
                codeforcesContest,
                solutionsInfo
        );
    }

    private static class CodeforcesSolutionInfo {

        public final String login;
        public final String problemName;
        public final Verdict verdict;
        public final int score;
        public final LocalDateTime dateTime;
        public final Language language;
        public final String code;

        public CodeforcesSolutionInfo(String login,
                                      String problemName,
                                      Verdict verdict,
                                      int score,
                                      LocalDateTime dateTime,
                                      Language language,
                                      String code) {
            this.login = login;
            this.problemName = problemName;
            this.verdict = verdict;
            this.score = score;
            this.dateTime = dateTime;
            this.language = language;
            this.code = code;
        }
    }

    private static List<CodeforcesSolutionInfo> readSolutionsInfo(CodeforcesContest codeforcesContest) {
        return IOUtils.getFiles(
                new File(codeforcesContest.getLocalDirectoryPath())
        ).stream().map(solutionFile -> {
            String fileName = solutionFile.getName();

            int lastDotIndex = fileName.lastIndexOf(".");
            String extension = fileName.substring(lastDotIndex + 1);

            Language language = Language.fromExtension(extension);

            String fileNameWithoutExtension = fileName.substring(0, lastDotIndex);

            int lastSpaceIndex = fileNameWithoutExtension.lastIndexOf("_");
            String problemName = fileNameWithoutExtension.substring(lastSpaceIndex + 1);

            String login = fileNameWithoutExtension.substring(0, lastSpaceIndex);

            try {
                String code = readCode(solutionFile);

                return new CodeforcesSolutionInfo(
                        login,
                        problemName,
                        Verdict.AC,
                        1,
                        codeforcesContest.getStartDateTime(),
                        language,
                        code
                );
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull)
        .collect(Collectors.toList());
    }

    private final CodeforcesContest contest;
    private final File localDirectory;
    private final List<CodeforcesSolutionInfo> solutionInfos;
    private final Map<String, List<Integer>> participantToSolutions;

    public CodeforcesLocalContestLoader(CodeforcesContest contest,
                                        List<CodeforcesSolutionInfo> solutionInfos) {
        this.contest = contest;
        this.localDirectory = new File(contest.getLocalDirectoryPath());
        this.solutionInfos = solutionInfos;

        this.participantToSolutions = new HashMap<>();
        for (int i = 0; i < solutionInfos.size(); ++i) {
            var info = solutionInfos.get(i);

            var solutions = participantToSolutions.computeIfAbsent(
                    info.login, (login) -> new ArrayList<>()
            );

            solutions.add(i);
        }
    }

    @Override
    public CodeforcesContest getContest() {
        return contest;
    }

    @Override
    public Set<Participant> loadParticipants(Contest contest) {
        return participantToSolutions.keySet().stream()
                .map(login -> new Participant(login, contest))
                .collect(Collectors.toSet());
    }

    @Override
    public ParticipantInfo loadParticipantInfo(Participant participant) {
        return ParticipantInfo.INFO_NOT_AVAILABLE;
    }

    private static String readCode(File solutionFile) throws IOException {
        return IOUtils.readFrom(
                new FileReader(solutionFile),
                (in) -> in.lines().collect(Collectors.joining("\n"))
        );
    }

    @Override
    public ParticipantSolutions loadSolutions(Participant participant) {
        var participantSolutions = new ParticipantSolutions();

        for (var solutionId : participantToSolutions.get(participant.login)) {
            var solutionInfo = solutionInfos.get(solutionId);

            if (solutionInfo.score == 0) continue;

            var solution = new Solution(
                    Integer.toString(solutionId),
                    participant,
                    solutionInfo.problemName,
                    SolutionProgram.create(solutionInfo.language, solutionInfo.code, solutionInfo.verdict),
                    solutionInfo.verdict,
                    solutionInfo.score,
                    solutionInfo.dateTime,
                    "1.2.3.4"
            );

            participantSolutions.addSolution(solution);
        }

        return participantSolutions;
    }
}
