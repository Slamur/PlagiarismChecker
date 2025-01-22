package com.slamur.plagiarism.service;

import com.slamur.plagiarism.model.parsing.contest.DirectoryContest;
import com.slamur.plagiarism.model.parsing.contest.SamaraContest;
import com.slamur.plagiarism.service.impl.*;
import com.slamur.plagiarism.service.impl.contest.DirectoryContestLoader;
import com.slamur.plagiarism.service.impl.contest.SamaraContestLoader;

public class Services {

    private static final Services instance = new Services();

    static {
        initializeServices();
    }

    private static void initializeServices() {
        properties().afterInitialization(credentials()::initialize);
        credentials().afterInitialization(contest()::initialize);
        contest().afterInitialization(comparisons()::initialize);
        comparisons().afterInitialization(verification()::initialize);
        verification().afterInitialization(interesting()::initialize);

        properties().initialize();
    }

    public static FxmlStageService fxml() { return instance.fxmlService; }

    public static PropertiesService properties() { return instance.propertiesService; }

    public static CredentialsService credentials() { return instance.credentialsService; }

    public static ContestService contest() {
        return instance.contestService;
    }

    public static ComparisonService comparisons() {
        return instance.comparisonService;
    }

    public static VerificationService verification() {
        return instance.verificationService;
    }

    public static InterestingComparisonsService interesting() { return instance.interestingComparisonsService; }

    private final FxmlStageService fxmlService;
    private final PropertiesService propertiesService;
    private final CredentialsService credentialsService;
    private final ContestService contestService;
    private final ComparisonService comparisonService;
    private final VerificationService verificationService;
    private final InterestingComparisonsService interestingComparisonsService;

    private Services() {
        this.fxmlService = new FxmlStageService();
        this.propertiesService = new PropertiesService();
        this.credentialsService = new CredentialsService();
        this.contestService = new ContestServiceImpl(
                DirectoryContestLoader.forContest(DirectoryContest.DS_PART_2_EXAM_24_25)
        );
        this.comparisonService = new ComparisonService();
        this.verificationService = new VerificationService();
        this.interestingComparisonsService = new InterestingComparisonsService();
    }
}
