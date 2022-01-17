package com.slamur.plagiarism.service.impl;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.participant.ParticipantResult;
import com.slamur.plagiarism.model.parsing.participant.ParticipantSolutions;
import com.slamur.plagiarism.model.parsing.participant.ProblemResult;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.service.ContestService;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.service.impl.contest.ContestLoader;
import com.slamur.plagiarism.service.impl.contest.ContestRepository;
import com.slamur.plagiarism.service.impl.contest.ParticipantRepository;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.DateTimeUtils;
import com.slamur.plagiarism.utils.IOUtils;

public class ContestServiceImpl extends ServiceBase implements ContestService {

    private final Contest contest;
    private final List<Participant> participants;
    private final Map<Participant, ParticipantInfo> infoByParticipant;
    private final Map<Participant, ParticipantSolutions> solutionsByParticipant;
    private final ContestLoader contestLoader;

    public ContestServiceImpl(ContestLoader contestLoader) {
        this.contest = contestLoader.getContest();
        this.contestLoader = contestLoader;

        this.participants = new ArrayList<>();
        this.infoByParticipant = new HashMap<>();
        this.solutionsByParticipant = new HashMap<>();
    }

    @Override
    protected void initializeOnly() {
        try {
            loadParticipants();
        } catch (Exception e) {
            AlertUtils.error("Не удалось загрузить участников с их решениями", e);
        }
    }

    @Override
    public Contest getContest() {
        return contest;
    }

    @Override
    public List<String> getProblems() {
        return contest.getProblems();
    }

    @Override
    public ParticipantInfo getInfo(Participant participant) {
        return infoByParticipant.get(participant);
    }

    private List<Solution> getSolutions(Participant participant) {
        return solutionsByParticipant.get(participant).getSolutions(
                contest.compareOnlyBestSolutions(),
                contest.getMinimalProblemsToCompare(),
                contest.getInterestingProblems()
        );
    }

    @Override
    public List<Solution> getSolutions() {
        return participants.stream()
                .map(this::getSolutions)
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    private ContestRepository getContestRepository() throws IOException {
        return ContestRepository.createRepository(contest);
    }

    @Override
    public File getDirectory() throws IOException {
        return getContestRepository().getDirectory();
    }

    private void loadParticipants() throws IOException {
        var contestParticipantsRepository = getContestRepository().createParticipantsRepository();

        Set<Participant> contestParticipants = new HashSet<>(
                contestParticipantsRepository.getCachedParticipants()
        );

        contestParticipants.addAll(
                contestLoader.loadParticipants(contest)
        );

        participants.clear();
        participants.addAll(contestParticipants);

        for (Participant participant : participants) {
            try {
                ParticipantRepository participantRepository = contestParticipantsRepository.createParticipantRepository(
                        participant
                );

                var info = participantRepository.loadInfo(contestLoader);
                infoByParticipant.put(participant, info);

                // TODO make all filters after loading?
                var solutions = participantRepository.loadSolutions(contestLoader);
                solutionsByParticipant.put(participant, solutions);
            } catch (Exception e) {
                AlertUtils.error(
                        String.format("Ошибка при загрузке информации и решений участника %s", participant.login),
                        e
                );
            }
        }

    }

    private List<ParticipantResult> calculateResults() {
        var verificication = Services.verification();

        // FIXME later
        return solutionsByParticipant.values().stream()
                .map(solutions -> solutions.getParticipantResults(contest, verificication::isPlagiat))
                .filter(participantResult -> !participantResult.getProblemResults().isEmpty())
                .filter(participantResult -> !participantResult.getParticipant().login.contains("INVALID TEAM"))
                .filter(participantResult -> !participantResult.getParticipant().login.contains("ejudge"))
                .filter(participantResult -> !participantResult.getParticipant().id.equals("CUser1415"))
                .filter(participantResult -> !participantResult.getParticipant().id.equals("CUser1289"))
                .filter(participantResult -> !participantResult.getParticipant().id.equals("AUser1662"))
                .filter(participantResult -> !participantResult.getParticipant().id.equals("DUser113"))
                .collect(Collectors.toList());
    }

    private void saveParticipants() {
        var sortedLogins = participants.stream()
                .map(Participant::getLogin)
                .filter(login -> List.of("ejudge", "INVALID").stream().noneMatch(login::contains))
                .map(login -> {
                    var type = login.charAt(0);
                    var number = Integer.parseInt(login.substring(1 + "User".length()));

                    return new Point(type, number);
                }).sorted((a, b) -> {
                    if (a.x != b.x) return a.x - b.x;
                    return a.y - b.y;
                }).map(p -> "" + (char)p.x + "User" + p.y)
                .collect(Collectors.toList());

        try {
            getContestRepository().saveParticipants(sortedLogins);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveStandings() throws IOException {
        saveParticipants(); // FIXME

        var standingsRepository = getContestRepository().createStandingsRepository();

        standingsRepository.saveStandings((out) -> {
            List<String> headerFixed = List.of(
                    "Place",
                    "User_Login",
                    "User_Name",
                    "Total_Score",
                    "Total_Penalty",
                    "Last_Accepted_Seconds"
            );

            List<String> headerProblems = contest.getProblems().stream()
                    .flatMap(problem -> Stream.of(
                            "Score_" + problem,
                            "Tries_" + problem,
                            "Time_" + problem,
                            "Penalty_" + problem
                    )).collect(Collectors.toList());

            List<String> header = Stream.of(
                    headerFixed, headerProblems
            ).flatMap(List::stream).collect(Collectors.toList());

            out.println(String.join(",", header));

            var results = calculateResults();
            Collections.sort(results);

            for (int place = 0; place < results.size(); ++place) {
                var result = results.get(place);

                var participant = result.getParticipant();

                var participantInfo = infoByParticipant.get(participant);
                var participantName = ParticipantInfo.NOT_AVAILABLE.equals(participantInfo.name)
                        ? ""
                        : participantInfo.name;

                List<String> rowFixed = List.of(
                        Integer.toString(place + 1),
                        participant.login,
                        participantName,
                        Long.toString(result.getTotalScore()),
                        Long.toString(result.getTotalPenaltyTime()),
                        Long.toString(result.getLastAcceptedTime()) // with seconds
                );

                var problemResults = result.getProblemResults();

                List<String> rowProblems = contest.getProblems().stream()
                        .map(problemResults::get)
                        .flatMap(problemResult -> {
                            if (ProblemResult.NOT_TRIED == problemResult) {
                                return Stream.of("", "", "", "");
                            } else {
                                return Stream.of(
                                        Long.toString(problemResult.getScore()),
                                        Long.toString(problemResult.getTries()),
                                        Long.toString(DateTimeUtils.toCeilingMinutes(problemResult.getTime())),
                                        Long.toString(problemResult.getPenalty())
                                );
                            }
                        })
                        .collect(Collectors.toList());

                List<String> row = Stream.of(
                        rowFixed, rowProblems
                ).flatMap(List::stream).collect(Collectors.toList());

                out.println(String.join(",", row));
            }
        });
    }
}
