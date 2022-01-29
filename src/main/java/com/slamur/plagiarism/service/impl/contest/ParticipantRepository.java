package com.slamur.plagiarism.service.impl.contest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.participant.ParticipantSolutions;
import com.slamur.plagiarism.model.parsing.solution.Language;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.SolutionProgram;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.utils.IOUtils;

import static com.slamur.plagiarism.utils.DateTimeUtils.DATE_TIME_FORMATTER;

public class ParticipantRepository {

    private final Participant participant;
    private final File directory;

    public ParticipantRepository(Participant participant, File directory) {
        this.participant = participant;
        this.directory = directory;
    }

    public ParticipantInfo loadInfo(ContestLoader contestLoader) throws IOException {
        File infoFile = new File(directory, "info.txt");
        if (!infoFile.exists()) {
            ParticipantInfo info = contestLoader.loadParticipantInfo(participant);
            info.saveToFile(infoFile);
            return info;
        } else {
            return ParticipantInfo.loadFromFile(infoFile);
        }
    }

    public ParticipantSolutions loadSolutions(ContestLoader contestLoader) throws IOException {
        File solutionsFile = new File(directory, "solutions.txt");
        if (!solutionsFile.exists()) {
            var solutions = contestLoader.loadSolutions(participant);
            saveSolutionsToFile(solutionsFile, solutions);
            return solutions;
        } else {
            return loadSolutionsFromFile(solutionsFile);
        }
    }

    private static void saveSolutionsToFile(File file, ParticipantSolutions participantSolutions)
            throws FileNotFoundException, UnsupportedEncodingException {
        IOUtils.saveToFile(file, (out) -> {
            var solutions = participantSolutions.getAllSolutions();
            out.println(solutions.size());
            for (Solution solution : solutions) {
                out.println(solution.toText());
            }
        });
    }

    private ParticipantSolutions loadSolutionsFromFile(File file) throws IOException {
        return IOUtils.loadFromFile(file, (in) -> {
            var participantSolutions = new ParticipantSolutions();

            int solutionsCount = Integer.parseInt(in.readLine());

            for (int solutionIndex = 0; solutionIndex < solutionsCount; ++solutionIndex) {
                String id = in.readLine();
                while (id.isEmpty()) id = in.readLine();

                String problemId = in.readLine();

                Verdict verdict = Verdict.valueOf(in.readLine());

                int score = Integer.parseInt(in.readLine());

                LocalDateTime dateTime = LocalDateTime.parse(
                        in.readLine(), DATE_TIME_FORMATTER
                );

                String ip = in.readLine();

                // TODO separate SolutionProgram reading
                Language language = Language.fromExtension(in.readLine());

                int codeSize = Integer.parseInt(in.readLine());
                StringBuilder codeBuilder = new StringBuilder();
                for (int j = 0; j < codeSize; ++j) {
                    codeBuilder.append(in.readLine()).append("\n");
                }

                var program = new SolutionProgram(language, codeBuilder.toString());

                in.readLine();

                participantSolutions.addSolution(
                        new Solution(
                                id,
                                participant,
                                problemId,
                                program,
                                verdict,
                                score,
                                dateTime,
                                ip
                        )
                );
            }

            return participantSolutions;
        });
    }
}