package com.slamur.plagiarism.service.impl;

import com.slamur.plagiarism.model.verification.Comparison;
import com.slamur.plagiarism.service.Services;
import com.slamur.plagiarism.utils.AlertUtils;
import com.slamur.plagiarism.utils.IOUtils;

import java.io.File;
import java.io.IOException;

public class InterestingComparisonsService extends ServiceBase {
    @Override
    protected void initializeOnly() {

    }

    private static boolean isInteresting(Comparison comparison) {
        int leftSize = comparison.left.getProgram().code.replace("\n\n", "\n").split("\n").length;
        int rightSize = comparison.right.getProgram().code.replace("\n\n", "\n").split("\n").length;

        return Math.max(leftSize, rightSize) > 4;
    }

    public void saveInteresting() {
        Services.comparisons().afterInitialization(() -> {
            var comparisons = Services.comparisons()
                   .filtered(Services.comparisons().moreThan(1.0))
                   .filtered(InterestingComparisonsService::isInteresting);

            try {
                AlertUtils.information(
                        "Обрабатывается %d сравнений".formatted(comparisons.size())
                );

                var contestDirectory = Services.contest().getDirectory();

                IOUtils.saveToFile(new File(contestDirectory, "interesting_found.txt"), out -> {
                    comparisons.forEach(comparison -> {
                        String leftName = comparison.left.getId() + "." + comparison.left.getProgram().languageAlias;
                        String rightName = comparison.right.getId() + "." + comparison.right.getProgram().languageAlias;

                        out.println(leftName + " VS " + rightName + ": " + Services.comparisons().getSimilarity(comparison));
                        out.println(comparison.left.getProgram().code);
                        out.println("===|||===");
                        out.println(comparison.right.getProgram().code);
                        out.println("=====================================================================================");
                    });
                });

                IOUtils.saveToFile(new File(contestDirectory, "interesting_found_names.tsv"), out -> {
                    comparisons.forEach(comparison -> {
                        String leftName = comparison.left.getId() + "." + comparison.left.getProgram().languageAlias;
                        String rightName = comparison.right.getId() + "." + comparison.right.getProgram().languageAlias;

                        out.println(leftName + "\t" + rightName);
                    });
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
