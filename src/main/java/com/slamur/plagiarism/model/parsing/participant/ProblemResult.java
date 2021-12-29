package com.slamur.plagiarism.model.parsing.participant;

import java.time.Duration;

public class ProblemResult {

    public static final ProblemResult NOT_TRIED = new ProblemResult(
            0, Duration.ZERO, 0, 0
    );

    private final int score;
    private final Duration time;
    private final int tries;
    private final long penalty;

    public ProblemResult(int score, Duration time, int tries, long penalty) {
        this.score = score;
        this.time = time;
        this.tries = tries;
        this.penalty = penalty;
    }

    public int getScore() {
        return score;
    }

    public Duration getTime() {
        return time;
    }

    public int getTries() {
            return tries;
        }

    public long getPenalty() {
        return penalty;
    }
}