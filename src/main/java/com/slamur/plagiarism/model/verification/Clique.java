package com.slamur.plagiarism.model.verification;

import java.util.ArrayList;
import java.util.List;

import com.slamur.plagiarism.model.parsing.Participant;

public class Clique {

    private final int problemId;
    private final List<Participant> participants;

    Clique(int problemId) {
        this.problemId = problemId;
        this.participants = new ArrayList<>();
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public int size() {
        return getParticipants().size();
    }

    public void mergeWith(Clique otherClique) {
        participants.addAll(otherClique.participants);
    }

    public String participantsToText() {
        var builder = new StringBuilder();
        for (Participant participant : participants) {
            builder.append(participant.toText()).append("\n");
        }

        return builder.toString();
    }

    public String solutionsToText() {
        var builder = new StringBuilder();

        for (Participant participant : participants) {
            builder.append(participant.login)
                    .append("\t")
                    .append(participant.solutions[problemId])
                    .append("\n");
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return participantsToText() + "\n" +
                solutionsToText() + "\n";
    }
}
