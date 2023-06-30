package com.googlesource.gerrit.plugins.chatgpt;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.client.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.OpenAiClient;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class PatchSetReviewer {
    private static final String SPLIT_REVIEW_MSG = "Too many changes. Please consider splitting into patches smaller than %s lines for review.";
    private static final int COMMENT_BATCH_SIZE = 200;
    private final GerritClient gerritClient;
    private final OpenAiClient openAiClient;

    @Inject
    PatchSetReviewer(GerritClient gerritClient, OpenAiClient openAiClient) {
        this.gerritClient = gerritClient;
        this.openAiClient = openAiClient;
    }

    public static String reducePatchSet(String patchSet) {
        Set<String> skipPrefixes = new HashSet<>(Arrays.asList(
                "import", "-", "+package", "+import", "From", "Date:", "Subject:",
                "Change-Id:", "diff --git", "index", "---", "+++", "@@", "Binary files differ"
        ));

        return Arrays.stream(patchSet.split("\n"))
                .map(String::trim)
                .filter(line -> skipPrefixes.stream().noneMatch(line::startsWith))
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.joining("\n"));
    }

    public void review(Configuration config, String fullChangeId) throws Exception {
        String patchSet = gerritClient.getPatchSet(config, fullChangeId);
        if (config.isPatchSetReduction()) {
            patchSet = reducePatchSet(patchSet);
            log.debug("Reduced patch set: {}", patchSet);
        }

        String reviewSuggestion = getReviewSuggestion(config, fullChangeId, patchSet);
        List<String> reviewBatches = splitReviewIntoBatches(reviewSuggestion);

        for (String reviewBatch : reviewBatches) {
            gerritClient.postComment(config, fullChangeId, reviewBatch);
        }
    }

    private List<String> splitReviewIntoBatches(String review) {
        List<String> batches = new ArrayList<>();
        String[] lines = review.split("\n");

        StringBuilder batch = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            batch.append(lines[i]).append("\n");
            if ((i + 1) % COMMENT_BATCH_SIZE == 0) {
                batches.add(batch.toString());
                batch = new StringBuilder();
            }
        }
        if (batch.length() > 0) {
            batches.add(batch.toString());
        }
        log.info("Review batches created: {}", batches.size());
        return batches;
    }

    private String getReviewSuggestion(Configuration config, String changeId, String patchSet) throws Exception {
        List<String> patchLines = Arrays.asList(patchSet.split("\n"));
        if (patchLines.size() > config.getMaxReviewLines()) {
            log.warn("Patch set too large. Skipping review. changeId: {}", changeId);
            return String.format(SPLIT_REVIEW_MSG, config.getMaxReviewLines());
        }
        return openAiClient.ask(config, patchSet);
    }
}

