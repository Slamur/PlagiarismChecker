package com.slamur.plagiarism.model.parsing.contest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.service.impl.contest.ContestLoader;
import com.slamur.plagiarism.service.impl.contest.SamaraContestLoader;

public class SamaraContest extends Contest {

    private static final String SCHOOL = "sch", CITY = "okrug", REGION = "vseros";

    public static final Contest OKRUG_2020 = new SamaraContest(
            CITY, 579,
            LocalDate.of(2020, 11, 21),
            LocalTime.of(10, 0),
            LocalTime.of(15, 0),
            problemsRange('A', 'E')
    );

    public static final Contest REGION_2020_1 = new SamaraContest(
            REGION, 581,
            LocalDate.of(2021, 1, 16),
            LocalTime.of(10, 35),
            LocalTime.of(15, 45),
            problemsRange('A', 'D')
    );

    public static final Contest REGION_2020_2 = new SamaraContest(
            REGION, 583,
            LocalDate.of(2021, 1, 18),
            LocalTime.of(10, 0),
            LocalTime.of(15, 6),
            problemsRange('E', 'H')
    );

    public static final Contest OKRUG_2021 = new SamaraContest(
            CITY, 588,
            LocalDate.of(2021, 11, 27),
            LocalTime.of(10, 0),
            LocalTime.of(14, 0),
            problemsRange('A', 'E')
    );

    public SamaraContest(String type,
                         int id,
                         LocalDate date,
                         LocalTime startTime,
                         LocalTime endTime,
                         List<String> problems) {
        super(type,
                id,
                date,
                startTime,
                endTime,
                problems,
                true,
                1
        );
    }

    @Override
    public String toText(Participant participant) {
        return SamaraContestLoader.getFullLink(participant);
    }

    @Override
    public String toText(Solution solution) {
        return SamaraContestLoader.getFullLink(solution);
    }
}