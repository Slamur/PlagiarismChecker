package com.slamur.plagiarism.model.parsing.participant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.slamur.plagiarism.model.parsing.Contest;
import com.slamur.plagiarism.model.parsing.Solution;
import com.slamur.plagiarism.utils.RequestUtils;

public class Participant {

    public static final String PROFILE_PREFIX = "/ru/viewprofile/";

    public static String getLinkByLogin(String login) {
        return PROFILE_PREFIX + login + "/all";
    }

    public static Participant createFromLink(String link, Contest contest) {
        if (link.endsWith("/")) link = link.substring(0, link.length() - 1);
        if (link.contains("%20")) {
            link = link.replace("%20", "");
        }

        return new Participant(link, contest.getProblemsCount());
    }

    // TODO separate participant and participant result (problemToBestSolutions)
    // store map <participant -> participant result>
    public final String link;
    public final String id;
    public final String login;
    public final Solution[] problemToBestSolution;
    public final List<Solution> allSolutions;

    private Participant(String link, int problemsCount) {
        this.link = link;

        String linkWithoutAll = link.substring(0, link.indexOf("/all"));
        this.login = linkWithoutAll.substring(linkWithoutAll.lastIndexOf("/") + 1);
        this.id = login.substring(login.indexOf("_") + 1);

        this.problemToBestSolution = new Solution[problemsCount];
        this.allSolutions = new ArrayList<>();
    }

    public void addSolution(Solution solution, int problemIndex) {
        allSolutions.add(solution);

        // compare with old
        Solution oldSolution = problemToBestSolution[problemIndex];

        boolean needUpdate = (null == oldSolution)
                || oldSolution.score < solution.score
                || oldSolution.score == solution.score
                    && oldSolution.dateTime.compareTo(solution.dateTime) > 0;

        if (needUpdate) {
            problemToBestSolution[problemIndex] = solution;
        }
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

    public String getFullLink() {
        return RequestUtils.DOMAIN + link;
    }

    public String toText() {
        return getFullLink();
    }
}
