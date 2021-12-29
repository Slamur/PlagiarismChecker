package com.slamur.plagiarism.model.parsing.participant;

import java.util.Objects;

import com.slamur.plagiarism.model.parsing.contest.Contest;

public class Participant {

    public final String id;
    public final String login;
    public final Contest contest;

    public Participant(String login, Contest contest) {
        this(login, login, contest);
    }

    public Participant(String login, String id, Contest contest) {
        this.login = login;
        this.id = id;
        this.contest = contest;
    }

    public String getLogin() {
        return login;
    }

    public Contest getContest() {
        return contest;
    }

    @Override
    public String toString() {
        return login;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Participant that = (Participant) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toText() {
        return contest.toText(this);
    }
}
