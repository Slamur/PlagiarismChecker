package com.slamur.plagiarism.service.impl.contest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

public class EjudgeLocalContestLoader implements ContestLoader {

    private static final String DASH = "-";
    private static final DateTimeFormatter SOLUTION_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static EjudgeLocalContestLoader forLocal(EjudgeContest ejudgeContest) {
        String localDirectoryPath = ejudgeContest.getLocalDirectoryPath();
        File localDirectory = new File(localDirectoryPath);
        if (!localDirectory.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format("Путь не соответствует директории: %s", localDirectoryPath)
            );
        }

        Map<Integer, EjudgeSolutionInfo> solutionsInfo = new HashMap<>();
        Map<String, ParticipantInfo> participantsInfo = new HashMap<>();

        readSolutionsInfo(
                ejudgeContest.getSolutionsInfoCsvPath(),
                solutionsInfo,
                participantsInfo
        );

        return new EjudgeLocalContestLoader(
                ejudgeContest,
                localDirectory,
                solutionsInfo,
                participantsInfo
        );
    }

    private static class EjudgeSolutionInfo {

        public final String login;
        public final String problemName;
        public final Verdict verdict;
        public final int score;
        public final LocalDateTime dateTime;
        public final Language language;

        public EjudgeSolutionInfo(String login,
                                  String problemName,
                                  Verdict verdict,
                                  int score,
                                  LocalDateTime dateTime,
                                  Language language) {
            this.login = login;
            this.problemName = problemName;
            this.verdict = verdict;
            this.score = score;
            this.dateTime = dateTime;
            this.language = language;
        }
    }

    private static void readSolutionsInfo(
            String solutionsInfoCsvPath,
            Map<Integer, EjudgeSolutionInfo> solutionsInfo,
            Map<String, ParticipantInfo> participantsInfo) {
        File solutionsInfoFile = new File(solutionsInfoCsvPath);
        if (!solutionsInfoFile.exists()) {
            return;
        }

        try {
            IOUtils.readFrom(new FileReader(solutionsInfoFile), (in) -> {
                final String separator = "[,;]";
                var header = in.readLine().split(separator); // TODO move to CsvUtils

                Map<String, Integer> nameToColumn = new HashMap<>();
                for (int i = 0; i < header.length; ++i) {
                    nameToColumn.put(header[i], i);
                }

                int idColumn = nameToColumn.get("Run_Id");
                int verdictColumn = nameToColumn.get("Stat_Short");
                int scoreColumn = nameToColumn.get("Score");
                int problemColumn = nameToColumn.get("Prob");
                int aliasColumn = nameToColumn.get("Lang");

                int participantsLoginColumn = nameToColumn.get("User_Login");
                int participantNameColumn = nameToColumn.get("User_Name");

                int yearColumn = nameToColumn.get("Year");
                int monthColumn = nameToColumn.get("Mon");
                int dayColumn = nameToColumn.get("Day");
                int hoursColumn = nameToColumn.get("Hour");
                int minutesColumn = nameToColumn.get("Min");
                int secondsColumn = nameToColumn.get("Sec");

                in.lines().forEach(line -> {
                    try {
                        var row = line.split(separator);

                        // TODO add map with starts/ends
                        Verdict verdict = Verdict.fromText(row[verdictColumn]);
                        if (Verdict.UNKNOWN == verdict) return;

                        var dateTime = LocalDateTime.of(
                                Integer.parseInt(row[yearColumn]),
                                Month.of(Integer.parseInt(row[monthColumn])),
                                Integer.parseInt(row[dayColumn]),
                                Integer.parseInt(row[hoursColumn]),
                                Integer.parseInt(row[minutesColumn]),
                                Integer.parseInt(row[secondsColumn])
                        );

                        String login = row[participantsLoginColumn];

                        int id = Integer.parseInt(row[idColumn]);
                        int score = (Verdict.AC == verdict) ? 1 : 0; //Integer.parseInt(row[scoreColumn]);
                        String problemName = row[problemColumn];

                        Language language = Language.fromAlias(row[aliasColumn]);

                        solutionsInfo.put(id, new EjudgeSolutionInfo(
                                login,
                                problemName,
                                verdict,
                                score,
                                dateTime,
                                language
                        ));

                        if (!participantsInfo.containsKey(login)) {
                            String name = row[participantNameColumn];
                            if (name.isBlank()) name = ParticipantInfo.NOT_AVAILABLE;

                            participantsInfo.put(
                                    login,
                                    new ParticipantInfo(
                                            name,
                                            ParticipantInfo.NOT_AVAILABLE,
                                            ParticipantInfo.NOT_AVAILABLE
                                    )
                            );
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                return null;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final EjudgeContest contest;
    private final File localDirectory;
    private final Map<Integer, EjudgeSolutionInfo> solutionsInfo;
    private final Map<String, ParticipantInfo> participantsInfo;
    private final Map<String, List<Integer>> participantToSolutions;

    public EjudgeLocalContestLoader(EjudgeContest contest,
                                    File localDirectory,
                                    Map<Integer, EjudgeSolutionInfo> solutionsInfo,
                                    Map<String, ParticipantInfo> participantsInfo) {
        this.contest = contest;
        this.localDirectory = localDirectory;
        this.solutionsInfo = solutionsInfo;
        this.participantsInfo = participantsInfo;

        this.participantToSolutions = new HashMap<>();
        solutionsInfo.forEach((id, info) -> {
            var solutions = participantToSolutions.computeIfAbsent(
                    info.login, (login) -> new ArrayList<>()
            );

            solutions.add(id);
        });
    }

    @Override
    public EjudgeContest getContest() {
        return contest;
    }

    @Override
    public Set<Participant> loadParticipants(Contest contest) {
        return participantsInfo.keySet().stream()
                .map(login -> new Participant(login, contest))
                .collect(Collectors.toSet());
    }

    @Override
    public ParticipantInfo loadParticipantInfo(Participant participant) {
        return participantsInfo.getOrDefault(participant.login, ParticipantInfo.INFO_NOT_AVAILABLE);
    }

    private String readCode(File solutionFile) {
        try {
            return IOUtils.loadFromFile(solutionFile, (in) -> {
                StringBuilder codeBuilder = new StringBuilder();
                in.lines().map(line -> line + "\n").forEach(codeBuilder::append);
                return codeBuilder.toString();
            });
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public ParticipantSolutions loadSolutions(Participant participant) {
        Map<Integer, Solution> solutionsWithCode = new HashMap<>();

        File participantDirectory = new File(localDirectory, participant.login);
        if (!participantDirectory.exists()) {
            return new ParticipantSolutions();
        }

        IOUtils.getFiles(participantDirectory).forEach(solutionFile -> {
            try {
                String solutionFileName = solutionFile.getName();

                int firstDash = solutionFileName.indexOf(DASH);
                String contestId = solutionFileName.substring(0, firstDash);
                if (contest.getId() != Integer.parseInt(contestId)) return;

                int secondDash = solutionFileName.indexOf(DASH, firstDash + 1);
                String solutionId = solutionFileName.substring(firstDash + 1, secondDash);

                int lastDash = solutionFileName.lastIndexOf(DASH);
                String participantLogin = solutionFileName.substring(secondDash + 1, lastDash);
                // TODO can be FIO instead of login
//            if (!participant.login.equals(participantLogin)) return;

                int dot = solutionFileName.indexOf(".");
                String dateTimeString = solutionFileName.substring(lastDash + 1, dot);
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, SOLUTION_DATE_TIME_FORMATTER);
                if (dateTime.compareTo(contest.getStartDateTime()) < 0
                        || contest.getEndDateTime().compareTo(dateTime) < 0) {
                    return;
                }

                Language language = Language.fromExtension(solutionFileName.substring(dot + 1));
                String code = readCode(solutionFile);
                var program = new SolutionProgram(language, code);

                int solutionIdInt = Integer.parseInt(solutionId);
                EjudgeSolutionInfo solutionInfo = solutionsInfo.get(solutionIdInt);
                if (null == solutionInfo) {
                    solutionsInfo.put(solutionIdInt,
                            solutionInfo = new EjudgeSolutionInfo(
                                    participantLogin,
                                    "",
                                    Verdict.DISQUALIFIED,
                                    0,
                                    dateTime,
                                    language
                            )
                    );
                }

                var solution = new Solution(
                        solutionId,
                        participant,
                        solutionInfo.problemName,
                        program,
                        solutionInfo.verdict,
                        solutionInfo.score,
                        dateTime
                );

                solutionsWithCode.put(Integer.parseInt(solutionId), solution);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        });

        var participantSolutions = new ParticipantSolutions();

        for (var solutionId : participantToSolutions.get(participant.login)) {
            var solution = solutionsWithCode.get(solutionId);
            if (null == solution) {
                var solutionInfo = solutionsInfo.get(solutionId);

                solution = new Solution(
                        // FIXME
                        Integer.toString(1000 * 1000 + solutionId).substring(1),
                        participant,
                        solutionInfo.problemName,
                        new SolutionProgram(solutionInfo.language, ""),
                        solutionInfo.verdict,
                        solutionInfo.score,
                        solutionInfo.dateTime
                );
            }

            participantSolutions.addSolution(solution);
        }

        return participantSolutions;
    }
}
