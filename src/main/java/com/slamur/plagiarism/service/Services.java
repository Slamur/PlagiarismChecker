package com.slamur.plagiarism.service;

import com.slamur.plagiarism.service.impl.ComparisonService;
import com.slamur.plagiarism.service.impl.ContestService;
import com.slamur.plagiarism.service.impl.CredentialsService;
import com.slamur.plagiarism.service.impl.FxmlStageService;
import com.slamur.plagiarism.service.impl.PropertiesService;
import com.slamur.plagiarism.service.impl.VerificationService;

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

    private final FxmlStageService fxmlService;
    private final PropertiesService propertiesService;
    private final CredentialsService credentialsService;
    private final ContestService contestService;
    private final ComparisonService comparisonService;
    private final VerificationService verificationService;

    private Services() {
        this.fxmlService = new FxmlStageService();
        this.propertiesService = new PropertiesService();
        this.credentialsService = new CredentialsService();
        this.contestService = new ContestService();
        this.comparisonService = new ComparisonService();
        this.verificationService = new VerificationService();
    }
}
