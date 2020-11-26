package com.slamur.plagiarism.service;

public class Services {

    private static final Services instance = new Services();

    static {
        initializeServices();
    }

    private static void initializeServices() {
        contest().afterInitialization(() -> {
            comparisons().initialize();
        });

        comparisons().afterInitialization(() -> {
            verification().initialize();
        });

        contest().initialize();
    }

    public static ContestService contest() {
        return instance.contestService;
    }

    public static ComparisonService comparisons() {
        return instance.comparisonService;
    }

    public static VerificationService verification() {
        return instance.verificationService;
    }

    private final ContestService contestService;
    private final ComparisonService comparisonService;
    private final VerificationService verificationService;

    private Services() {
        this.contestService = new ContestService();
        this.comparisonService = new ComparisonService();
        this.verificationService = new VerificationService();
    }
}
