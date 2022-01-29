package com.slamur.plagiarism.model.parsing.solution;

import java.time.LocalDateTime;
import java.util.Objects;

import com.slamur.plagiarism.model.parsing.participant.Participant;

import static com.slamur.plagiarism.utils.DateTimeUtils.DATE_TIME_FORMATTER;

public class Solution implements Comparable<Solution> {

    public final Participant participant;
    public final String id;
    public final String problemName;
    private final SolutionProgram program;
    public final Verdict verdict;
    public final int score;
    public final LocalDateTime dateTime;
    public final String ip;

    public Solution(String id,
                    Participant participant,
                    String problemName,
                    SolutionProgram program,
                    Verdict verdict,
                    int score,
                    LocalDateTime dateTime,
                    String ip) {
        this.id = id;
        this.participant = participant;
        this.problemName = problemName;
        this.program = program;
        this.verdict = verdict;
        this.score = score;
        this.dateTime = dateTime;
        this.ip = ip;
    }

    public String getId() {
        return id;
    }

    public Participant getParticipant() {
        return participant;
    }

    public String getProblemName() { return problemName; }

    public SolutionProgram getProgram() {
        return program;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public String toString() {
        return getFullLink() + "\t" + getDateTimeString();
    }

    public String getFullLink() {
        return participant.getContest().toText(this);
    }

    public String getDateTimeString() {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public String toText() {
        return id + "\n" +
                problemName + "\n" +
                verdict + "\n" +
                score + "\n" +
                getDateTimeString() + "\n" +
                ip + "\n" +
                program;
    }

    @Override
    public int compareTo(Solution other) {
        if (null == other) return 1;
        if (this.score != other.score) return Integer.compare(this.score, other.score);
        return other.dateTime.compareTo(this.dateTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Solution solution = (Solution) o;
        return Objects.equals(id, solution.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
