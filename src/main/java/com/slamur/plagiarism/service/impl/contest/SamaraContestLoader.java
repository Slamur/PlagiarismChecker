package com.slamur.plagiarism.service.impl.contest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.slamur.plagiarism.model.parsing.contest.Contest;
import com.slamur.plagiarism.model.parsing.contest.SamaraContest;
import com.slamur.plagiarism.model.parsing.participant.Participant;
import com.slamur.plagiarism.model.parsing.participant.ParticipantInfo;
import com.slamur.plagiarism.model.parsing.participant.ParticipantSolutions;
import com.slamur.plagiarism.model.parsing.solution.Solution;
import com.slamur.plagiarism.model.parsing.solution.SolutionProgram;
import com.slamur.plagiarism.model.parsing.solution.Verdict;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.ParsingUtils;
import com.slamur.plagiarism.utils.RequestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import static com.slamur.plagiarism.utils.DateTimeUtils.DATE_TIME_FORMATTER;
import static com.slamur.plagiarism.utils.DateTimeUtils.DATE_TIME_PATTERN;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

public class SamaraContestLoader implements ContestLoader {

    private static final String HREF_TAG_NAME = "href";

    public static final String DOMAIN = "http://contest.samsu.ru";
    private static final String USER_AGENT = "Mozilla/5.0";

    public static final String PROFILE_PREFIX = "/ru/viewprofile";
    public static final String SOLUTION_PREFIX = "/ru/solution";

    private static class SamaraContestRequestProcessor {

        //Cookie: sessionID=login%09password
        private Map<String, String> getCookies() {
            return Services.credentials().getCredentialCookies();
        }

        public String get(String link) throws IOException {
            return RequestUtils.request(
                    RequestUtils.GET,
                    link,
                    DOMAIN,
                    emptyMap(),
                    getCookies()
            );
        }

        public String monitor(Contest contest) throws IOException {
            return get("/ru/monitorvseros/" + contest.getId() + "/1");
        }
    }

    private interface ParticipantPageHolder {

        Document getPage() throws IOException;
    }

    private class ParticipantPageLazyHolder implements ParticipantPageHolder {

        private final String participantLink;
        private Document participantPage;

        public ParticipantPageLazyHolder(Participant participant) {
            this.participantLink = getFullLink(participant);
            this.participantPage = null;
        }

        @Override
        public Document getPage() throws IOException {
            if (null == participantPage) {
                String profilePlain = requestProcessor.get(participantLink);
                this.participantPage = Jsoup.parse(profilePlain);
            }

            return participantPage;
        }
    }

    private final SamaraContest contest;
    private final SamaraContestRequestProcessor requestProcessor;
    private final Map<Participant, ParticipantPageHolder> participantPageHolders;

    public SamaraContestLoader(SamaraContest contest) {
        this.contest = contest;
        this.requestProcessor = new SamaraContestRequestProcessor();
        this.participantPageHolders = new HashMap<>();
    }

    @Override
    public SamaraContest getContest() {
        return contest;
    }

    public static String getFullLinkWithDomain(Participant participant) {
        return DOMAIN + getFullLink(participant);
    }

    public static String getFullLink(Participant participant) {
        return PROFILE_PREFIX + "/" + participant.login + "/all";
    }

    public static String getFullLink(Solution solution) {
        return DOMAIN + SOLUTION_PREFIX + "/" + solution.getId();
    }

    public static Participant createParticipantFromLink(String link, Contest contest) {
        if (link.endsWith("/")) link = link.substring(0, link.length() - 1);
        if (link.contains("%20")) {
            link = link.replace("%20", "");
        }

        String linkWithoutAll = link.substring(0, link.indexOf("/all"));
        String login = linkWithoutAll.substring(linkWithoutAll.lastIndexOf("/") + 1);
        String id = login.substring(login.indexOf("_") + 1);

        return new Participant(login, contest);
    }

    @Override
    public Set<Participant> loadParticipants(Contest contest) {
        try {
            String monitorPlain = requestProcessor.monitor(contest);
            Document monitor = Jsoup.parse(monitorPlain);

            List<Element> participantLinks = monitor.getElementsByAttributeValueContaining(
                    HREF_TAG_NAME,
                    PROFILE_PREFIX + "/" + contest.getType()
            );

            return participantLinks.stream()
                    .map(participantLinkElement -> participantLinkElement.attr(HREF_TAG_NAME) + "all")
                    .map(link -> createParticipantFromLink(link, contest))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return emptySet();
        }
    }

    private Optional<Document> getParticipantPage(Participant participant) {
        var pageHolder = participantPageHolders.computeIfAbsent(
                participant, ParticipantPageLazyHolder::new
        );

        try {
            return Optional.of(pageHolder.getPage());
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public ParticipantInfo loadParticipantInfo(Participant participant) {
        return getParticipantPage(participant).map(participantPage -> {
            String name = ParsingUtils.parseField(participantPage, "Name");
            String school = ParsingUtils.parseField(participantPage, "School");
            String city = ParsingUtils.parseField(participantPage, "City");

            return new ParticipantInfo(name, school, city);
        }).orElse(ParticipantInfo.INFO_NOT_AVAILABLE);
    }

    @Override
    public ParticipantSolutions loadSolutions(Participant participant) {
        ParticipantSolutions participantSolutions = new ParticipantSolutions();

        getParticipantPage(participant).ifPresent(participantPage -> {
            List<Element> submits = participantPage.getElementsByTag("tr");

            Collections.reverse(submits);

            for (Element submitElement : submits) {
                try {
                    processSubmit(participant, submitElement, requestProcessor).ifPresent(
                            participantSolutions::addSolution
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return participantSolutions;
    }

    private Optional<Solution> processSubmit(Participant participant,
                               Element submitElement,
                               SamaraContestRequestProcessor requestProcessor) throws IOException {
        if (submitElement.getElementsByTag("th").size() > 0) return Optional.empty();

        Element submitLinkElement = submitElement.getElementsByAttributeValueContaining(HREF_TAG_NAME, SOLUTION_PREFIX).first();
        if (null == submitLinkElement) return Optional.empty();

        String submitLink = submitLinkElement.attr(HREF_TAG_NAME);

        String submitPlain = requestProcessor.get(submitLink);
        Document submitPage = Jsoup.parse(submitPlain);

        // parsing problem id
        Element problemNameElement = submitElement.getElementsByAttributeValueContaining(HREF_TAG_NAME, "problemset").first();

        String problemName = problemNameElement.text();
        // FIXME later
//        if (!problemName.matches() && !problemName.matches("^[1-9].*")) return Optional.empty();

        Matcher letterMatcher = Pattern.compile("[A-Z]").matcher(problemName);
        if (!letterMatcher.find()) return Optional.empty();
        String problemLetter = letterMatcher.group(0);

        var contest = participant.getContest();

        if (!contest.getProblems().contains(problemLetter)) {
            return Optional.empty();
        }

        // parsing verdict
        Element samplesVerdictElement = submitPage.getElementsByAttributeValueContaining(HREF_TAG_NAME, "resultshelp").first();
        String samplesVerdictText = samplesVerdictElement.text();

        Verdict verdict = Verdict.fromText(samplesVerdictText);

        int score = 0;
        if (verdict != Verdict.CE && verdict != Verdict.UNKNOWN) {
            // parsing score
            final String scoreSuffix = "баллов";
            Element realResultElement = submitPage.getElementsContainingOwnText(scoreSuffix).first();

            if (null != realResultElement) {
                String realResultText = realResultElement.ownText();

                int scoreSuffixIndex = realResultText.indexOf(scoreSuffix);

                String scorePrefixText = realResultText.substring(0, scoreSuffixIndex).trim();

                int lastSpaceIndex = scorePrefixText.lastIndexOf(" ");

                String scoreText = scorePrefixText.substring(lastSpaceIndex).trim();
                scoreText = scoreText.substring(0, scoreText.indexOf("."));
                score = Integer.parseInt(scoreText);
            }
        }

        if (score == 0) return Optional.empty();

        // parsing time
        Element timeElement = submitPage.getElementsContainingOwnText("Отправлено").first();
        if (timeElement == null) return Optional.empty();

        String fullTimeText = timeElement.text().trim();

        // TODO separate utils format and samara format
        String dateTimeText = fullTimeText.substring(
                fullTimeText.length() - DATE_TIME_PATTERN.length()
        ).trim();

        // TODO separate utils format and samara format
        LocalDateTime dateTime = LocalDateTime.parse(
                dateTimeText, DATE_TIME_FORMATTER
        );

        if (dateTime.compareTo(contest.getStartDateTime()) < 0) {
            return Optional.empty();
        }

        if (dateTime.compareTo(contest.getEndDateTime()) >= 0) {
            return Optional.empty();
        }

        // parsing language
        // FIXME won't work, need update
        Element languageElement = submitPage.getElementsContainingOwnText("Компилятор").first();
        String languageString = Optional.ofNullable(languageElement)
                .map(Element::text)
                .map(String::trim)
                .map(fullLanguageLine -> {
                    int space = fullLanguageLine.lastIndexOf(" ");
                    if (-1 == space) return "UNKNOWN";
                    return fullLanguageLine.substring(space + 1);
                }).orElse("UNKNOWN");

        Element codeElement = submitPage.getElementsByTag("code").first();

        String code = Parser.unescapeEntities(codeElement.text(), true);

        var program = SolutionProgram.create(languageString, code, verdict);

        String solutionId = submitLink.substring(
                submitLink.indexOf(SOLUTION_PREFIX + "/") + (SOLUTION_PREFIX + "/").length()
        );

        if (solutionId.endsWith("/")) solutionId = solutionId.substring(0, solutionId.length() - 1);

        return Optional.of(
            new Solution(
                solutionId,
                participant,
                problemLetter,
                program,
                verdict,
                score,
                dateTime,
                    ""
            )
        );
    }
}
