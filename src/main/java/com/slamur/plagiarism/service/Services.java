package com.slamur.plagiarism.service;

public class Services {

    public static ComparisonService comparisons() {
        if (null == instance.comparisonService) {
            synchronized (instance) {
                if (null == instance.comparisonService) {
                    instance.comparisonService = new ComparisonService();
                    instance.comparisonService.initialize();
                }
            }
        }

        return instance.comparisonService;
    }

    public static ContestService contest() {
        if (null == instance.contestService) {
            synchronized (instance) {
                if (null == instance.contestService) {
                    instance.contestService = new ContestService();
                    instance.contestService.initialize();
                }
            }
        }

        return instance.contestService;
    }

    public static VerificationService verification() {
        if (null == instance.verificationService) {
            synchronized (instance) {
                if (null == instance.verificationService) {
                    instance.verificationService = new VerificationService();
                    instance.verificationService.initialize();
                }
            }
        }

        return instance.verificationService;
    }

    private static final Services instance = new Services();

    private volatile ContestService contestService;
    private volatile ComparisonService comparisonService;
    private volatile VerificationService verificationService;

    private Services() {

    }
}
