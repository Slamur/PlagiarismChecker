package com.slamur.plagiarism.service.impl.contest;

import java.util.Set;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.participant.ParticipantSolutions;

public interface ContestLoader {

    Contest getContest();
    
    Set<Participant> loadParticipants(Contest contest);

    ParticipantInfo loadParticipantInfo(Participant participant);

    ParticipantSolutions loadSolutions(Participant participant);
}