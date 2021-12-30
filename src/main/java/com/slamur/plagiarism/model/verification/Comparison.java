package com.slamur.plagiarism.model.verification;

import java.util.Objects;

import com.slamur.plagiarism.model.IdsPair;
import com.slamur.plagiarism.model.parsing.solution.Solution;

public class Comparison {

    public final Solution left, right;

    public Comparison(Solution left, Solution right) {
        this.left = left;
        this.right = right;
    }

    public String getProblemName() {
        return left.problemName;
    }

    public String toText() {
        return String.format(
                "%s (%s) - %s (%s)",
                left.id, left.getParticipant().login,
                right.id, right.getParticipant().login
        );
    }

    @Override
    public String toString() {
        return left.id + " - " + right.id;
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
        return Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    public IdsPair toIds() {
        return new IdsPair(left.id, right.id);
    }

    public IdsPair toParticipantIds() {
        return new IdsPair(
            left.getParticipant().id,
            right.getParticipant().id
        );
    }
}
