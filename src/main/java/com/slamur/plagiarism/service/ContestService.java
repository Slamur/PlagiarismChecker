package com.slamur.plagiarism.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.solution.Solution;

public interface ContestService extends Service {

    Contest getContest();

    List<String> getProblems();

    ParticipantInfo getInfo(Participant participant);

    List<Solution> getSolutions();

    File getDirectory() throws IOException;

    void saveStandings() throws IOException;
}
