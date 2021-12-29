package com.slamur.plagiarism.model.parsing.participant;

import java.time.Duration;
import java.util.Map;

public class ParticipantResult implements Comparable<ParticipantResult> {

    private final Participant participant;
    private final Map<String, ProblemResult> problemResults;
    private final long totalScore, totalPenaltyTime;
    private final long lastAcceptedTime;

    public ParticipantResult(Participant participant,
                             Map<String, ProblemResult> problemResults) {

        this.participant = participant;
        this.problemResults = problemResults;

        this.totalScore = problemResults.values().stream()
                .mapToInt(ProblemResult::getScore)
                .sum();

        this.totalPenaltyTime = problemResults.values().stream()
                .filter(problemResult -> problemResult.getScore() > 0)
                .mapToLong(ProblemResult::getPenalty)
                .sum();

        this.lastAcceptedTime = problemResults.values().stream()
                .map(ProblemResult::getTime)
                .mapToLong(Duration::toSeconds)
                .max()
                .orElse(Long.MAX_VALUE);
    }

    public Participant getParticipant() {
        return participant;
    }

    public Map<String, ProblemResult> getProblemResults() {
        return problemResults;
    }

    public long getTotalScore() {
        return totalScore;
    }

    public long getTotalPenaltyTime() {
        return totalPenaltyTime;
    }

    public long getLastAcceptedTime() {
        return lastAcceptedTime;
    }

    @Override
    public int compareTo(ParticipantResult other) {
        int scoreCmp = Long.compare(this.totalScore, other.totalScore);
        if (0 != scoreCmp) {
            return -scoreCmp;
        }

        int penaltyCmp = Long.compare(this.totalPenaltyTime, other.totalPenaltyTime);
        if (0 != penaltyCmp) {
            return penaltyCmp;
        }

        return Long.compare(this.lastAcceptedTime, other.lastAcceptedTime);
    }
}