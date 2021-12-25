package com.slamur.plagiarism.model.verification;

import java.util.Objects;

import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.utils.ModelUtils;

public class Comparison {

    public final Participant left, right;

    public final int problemId;

    public Comparison(Participant left, Participant right, int problemId) {
        if (left.id.compareTo(right.id) > 0) {
            Participant tmp = left;
            left = right;
            right = tmp;
        }

        this.left = left;
        this.right = right;
        this.problemId = problemId;
    }

    public String getProblemName() {
        return ModelUtils.getProblemName(problemId);
    }

    @Override
    public String toString() {
        return String.format(
                "%s - %s (%s)",
                left.id, right.id, getProblemName()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Comparison that = (Comparison) o;
        return problemId == that.problemId &&
                Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, problemId);
    }
}
