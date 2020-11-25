package com.slamur.plagiarism.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.slamur.plagiarism.model.parsing.Contest;
import com.slamur.plagiarism.model.parsing.Participant;
import com.slamur.plagiarism.model.parsing.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.Solution;
import com.slamur.plagiarism.model.parsing.Verdict;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.IOUtils;
import com.slamur.plagiarism.utils.ParsingUtils;
import com.slamur.plagiarism.utils.RequestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public class ContestService implements Service {

    public static final Contest OKRUG_2020 = new Contest(
            Contest.CITY, 579,
            LocalDate.of(2020, 11, 21),
            LocalTime.of(10, 0),
            LocalTime.of(15, 0),
            5
    );

    private final Contest contest;
    private final List<Participant> participants;
    private final Map<Participant, ParticipantInfo> infoByParticipant;

    public ContestService() {
        this.contest = OKRUG_2020;
        this.participants = new ArrayList<>();
        this.infoByParticipant = new HashMap<>();
    }

    public void initialize() {
        try {
            participants.addAll(
                    loadParticipants()
            );
        } catch (IOException e) {
            AlertUtils.error("Не удалось загрузить участников с их решениями", e);
        }
    }

    public Contest getContest() {
        return contest;
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public ParticipantInfo getInfo(Participant participant) {
        return infoByParticipant.get(participant);
    }

    public int getProblemsCount() {
        return contest.getProblemsCount();
    }

    private class ContestRequestProcessor {

        private final Map<String, String> cookies;

        public ContestRequestProcessor(Map<String, String> cookies) {
            this.cookies = cookies;
        }

        public String get(String link) throws IOException {
            return RequestUtils.get(link, null, cookies);
        }

        public String monitor() throws IOException {
            return get("/ru/monitorvseros/" + contest.getId() + "/1");
        }
    }


    public List<Participant> loadParticipants() throws IOException {
        ContestRequestProcessor requestProcessor = new ContestRequestProcessor(
                CredentialsService.getCredentialCookies()
        );

        String monitorPlain = requestProcessor.monitor();
        Document monitor = Jsoup.parse(monitorPlain);

        List<Element> participantLinks = monitor.getElementsByAttributeValueContaining("href", "viewprofile/" + contest.getType());

        List<Participant> participants = new ArrayList<>();

        File contestFolder = contest.createFolder();
        File participantsFolder = new File(contestFolder, "participants");
        if (!participantsFolder.exists()) {
            if (!participantsFolder.mkdir()) {
                throw new IOException("Ошибка при создании папки с участниками");
            }
        }

        for (Element participantLinkElement : participantLinks) {

            String participantLink = participantLinkElement.attr("href");

            String profilePlain = requestProcessor.get(participantLink);
            Document participantPage = Jsoup.parse(profilePlain);

            Participant participant = Participant.create(participantLink, contest);

            File participantFolder = new File(participantsFolder, participant.login);
            if (!participantFolder.exists()) {
                if (!participantFolder.mkdir()) {
                    throw new IOException(String.format(
                            "Ошибка при создании папки участника %s",
                            participant.login
                    ));
                }
            }

            ParticipantInfo info = getInfo(participantFolder, participantPage);
            infoByParticipant.put(participant, info);

            fillSolutions(participant, participantFolder, participantPage, requestProcessor);

            participant.build();
            participants.add(participant);
        }

        return participants;
    }

    private void fillSolutions(Participant participant, File participantFolder, Document participantPage,
                               ContestRequestProcessor requestProcessor) throws IOException {
        File solutionsFile = new File(participantFolder, "solutions.txt");
        if (!solutionsFile.exists()) {
            loadSolutionsFromDomain(participant, participantPage, requestProcessor);

            saveSolutionsToFile(solutionsFile, participant);
        } else {
            loadSolutionsFromFile(solutionsFile, participant);
        }
    }

    private ParticipantInfo getInfo(File participantFolder, Document participantPage) throws IOException {
        File infoFile = new File(participantFolder, "info.txt");
        if (!infoFile.exists()) {
            ParticipantInfo info = parseInfoFromPage(participantPage);
            saveInfoToFile(infoFile, info);
            return info;
        } else {
            return loadInfoFromFile(infoFile);
        }
    }

    private void saveInfoToFile(File infoFile, ParticipantInfo info) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter out = new PrintWriter(infoFile, IOUtils.RUSSIAN_ENCODING)) {
            out.println(info.name);
            out.println(info.school);
            out.println(info.city);
        }
    }

    private ParticipantInfo loadInfoFromFile(File infoFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                IOUtils.createReader(new FileInputStream(infoFile))
                )
        ) {
            String name = reader.readLine();
            String school = reader.readLine();
            String city = reader.readLine();

            return new ParticipantInfo(name, school, city);
        }
    }

    private ParticipantInfo parseInfoFromPage(Document participantPage) {
        String name = ParsingUtils.parseField(participantPage, "Name");
        String school = ParsingUtils.parseField(participantPage, "School");
        String city = ParsingUtils.parseField(participantPage, "City");

        return new ParticipantInfo(name, school, city);
    }

    private void loadSolutionsFromDomain(Participant participant,
                                         Document participantPage,
                                         ContestRequestProcessor requestProcessor
    ) throws IOException {
        List<Element> submits = participantPage.getElementsByTag("tr");

        for (Element submitElement : submits) {
            processSubmit(contest, participant, submitElement, requestProcessor);
        }
    }

    private void processSubmit(Contest contest,
                               Participant participant,
                               Element submitElement,
                               ContestRequestProcessor requestProcessor) throws IOException {
        if (submitElement.getElementsByTag("th").size() > 0) return;

        Element submitLinkElement = submitElement.getElementsByAttributeValueContaining("href", "solution").first();
        String submitLink = submitLinkElement.attr("href");

        String submitPlain = requestProcessor.get(submitLink);
        Document submitPage = Jsoup.parse(submitPlain);

        Element timeElement = submitPage.getElementsContainingOwnText("Отправлено").first();
        if (timeElement == null) return;

        String fullTimeText = timeElement.text().trim();

        String dateTimeText = fullTimeText.substring(
                fullTimeText.length() - Solution.DATE_TIME_PATTERN.length()
        ).trim();

        LocalDateTime dateTime = LocalDateTime.parse(
                dateTimeText, Solution.DATE_TIME_FORMATTER
        );

        if (dateTime.compareTo(contest.getEndDateTime()) >= 0) {
            return;
        }

        if (dateTime.compareTo(contest.getStartDateTime()) < 0) {
            return;
        }

        Element problemNameElement = submitElement.getElementsByAttributeValueContaining("href", "problemset").first();

        String problemName = problemNameElement.text();
        if (problemName.length() != 1) return;

        int problemIndex = problemName.charAt(0) - 'A';

        Element samplesVerdictElement = submitPage.getElementsByAttributeValueContaining("href", "resultshelp").first();
        String samplesVerdictText = samplesVerdictElement.text();

        Verdict verdict = Verdict.fromText(samplesVerdictText);
        if (verdict == Verdict.CE || verdict == Verdict.UNKNOWN) return;

        final String scoreSuffix = "баллов";
        Element realResultElement = submitPage.getElementsContainingOwnText(scoreSuffix).first();

        int score = 0;
        if (null != realResultElement) {
            String realResultText = realResultElement.ownText();

            int scoreSuffixIndex = realResultText.indexOf(scoreSuffix);

            String scorePrefixText = realResultText.substring(0, scoreSuffixIndex).trim();

            int lastSpaceIndex = scorePrefixText.lastIndexOf(" ");

            String scoreText = scorePrefixText.substring(lastSpaceIndex).trim();
            scoreText = scoreText.substring(0, scoreText.indexOf("."));
            score = Integer.parseInt(scoreText);
        }

        Solution solution = participant.solutions[problemIndex];

        boolean needUpdate = (null == solution)
                || (Verdict.AC == verdict && Verdict.AC != solution.verdict)
                || solution.verdict == verdict && solution.dateTime.compareTo(dateTime) < 0;

        if (needUpdate) {
            Element codeElement = submitPage.getElementsByTag("code").first();

            String code = Parser.unescapeEntities(codeElement.text(), true);

            participant.solutions[problemIndex] = new Solution(submitLink, code, verdict, score, dateTime);
        }
    }

    private static void saveSolutionsToFile(File file, Participant participant) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(file)) {
            for (Solution solution : participant.solutions) {
                out.println(solution == null ? null : solution.toText());
            }
        }
    }

    private static void loadSolutionsFromFile(File file, Participant participant) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            for (int problemIndex = 0; problemIndex < participant.solutions.length; ++problemIndex) {
                String link = in.readLine();
                if ("null".equals(link)) continue;

                Verdict verdict = Verdict.valueOf(in.readLine());

                int score = Integer.parseInt(in.readLine());

                LocalDateTime dateTime = LocalDateTime.parse(
                        in.readLine(), Solution.DATE_TIME_FORMATTER
                );

                int codeSize = Integer.parseInt(in.readLine());
                StringBuilder codeBuilder = new StringBuilder();
                for (int j = 0; j < codeSize; ++j) {
                    codeBuilder.append(in.readLine()).append("\n");
                }

                in.readLine();

                participant.solutions[problemIndex] =
                        new Solution(link, codeBuilder.toString(), verdict, score, dateTime);
            }
        }
    }
}
