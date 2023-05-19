package com.slamur.plagiarism.service.impl.contest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.parsing.contest.CodeforcesContest;
import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.contest.NlogNContest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.participant.ParticipantSolutions;
import com.slamur.plagiarism.model.parsing.solution.Language;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.SolutionProgram;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.utils.IOUtils;

public class NlogNLocalContestLoader implements ContestLoader {

    public static NlogNLocalContestLoader forLocal(NlogNContest nlognContest) {
        var solutionsInfo = readSolutionsInfo(nlognContest);

        return new NlogNLocalContestLoader(
                nlognContest,
                solutionsInfo
        );
    }

    private static class NlogNSolutionInfo {

        public final String login;
        public final String problemName;
        public final Verdict verdict;
        public final String submitId;
        public final int score;
        public final LocalDateTime dateTime;
        public final Language language;
        public final String code;

        public NlogNSolutionInfo(String login,
                                  String problemName,
                                  Verdict verdict,
                                  String submitId,
                                  int score,
                                  LocalDateTime dateTime,
                                  Language language,
                                  String code) {
            this.login = login;
            this.problemName = problemName;
            this.verdict = verdict;
            this.submitId = submitId;
            this.score = score;
            this.dateTime = dateTime;
            this.language = language;
            this.code = code;
        }
    }

    private static List<NlogNSolutionInfo> readSolutionsInfo(NlogNContest nlogNContest) {
        return IOUtils.getFiles(
                nlogNContest.getLocalDirectory()
        ).stream().flatMap(problemDirectory -> IOUtils.getFiles(
            problemDirectory
        ).stream().map(solutionFile -> {
                        String fileName = solutionFile.getName();

                        int lastDotIndex = fileName.lastIndexOf(".");
                        String extension = fileName.substring(lastDotIndex + 1);

                        Language language = Language.fromExtension(extension);

                        String problemName = problemDirectory.getName();

                        String fileNameWithoutExtension = fileName.substring(0, lastDotIndex);

                        int lastSpaceIndex = fileNameWithoutExtension.lastIndexOf("_");

                        String submitId = fileNameWithoutExtension.substring(lastSpaceIndex + 1);

                        String login = fileNameWithoutExtension.substring(0, lastSpaceIndex);

                        try {
                            String code = readCode(solutionFile);

                            return new NlogNSolutionInfo(
                                    login,
                                    problemName,
                                    Verdict.AC,
                                    submitId,
                                    1,
                                    nlogNContest.getStartDateTime(),
                                    language,
                                    code
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                ).filter(Objects::nonNull)
        ).collect(Collectors.toList());
    }

    private final NlogNContest contest;
    private final File localDirectory;
    private final List<NlogNSolutionInfo> solutionInfos;
    private final Map<String, List<Integer>> participantToSolutions;

    public NlogNLocalContestLoader(NlogNContest contest,
                                   List<NlogNSolutionInfo> solutionInfos) {
        this.contest = contest;
        this.localDirectory = contest.getLocalDirectory();
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
    public NlogNContest getContest() {
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

            var solution = new Solution(
//                    Integer.toString(solutionId),
                    solutionInfo.submitId,
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
