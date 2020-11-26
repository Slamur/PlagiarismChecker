package com.slamur.plagiarism.model.parsing;

import java.util.Objects;

import com.slamur.plagiarism.utils.RequestUtils;

public class Participant {

    public static final String PROFILE_PREFIX = "/ru/viewprofile/";

    public static String getLinkByLogin(String login) {
        return PROFILE_PREFIX + login + "/";
    }

    public static Participant createFromLink(String link, Contest contest) {
        link = link.substring(0, link.length() - 1);
        if (link.endsWith("%20")) link = link.substring(0, link.indexOf("%20"));

        return new Participant(link, contest.getProblemsCount());
    }

    public final String link;
    public final int id;
    public final String login;
    public final Solution[] solutions;

    private Participant(String link, int problemsCount) {
        this.link = link;

        this.login = link.substring(link.lastIndexOf("/") + 1);
        this.id = Integer.parseInt(login.substring(login.indexOf("_") + 1));

        this.solutions = new Solution[problemsCount];
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
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getFullLink() {
        return RequestUtils.DOMAIN + link;
    }

    public String toText() {
        return getFullLink();
    }
}
