package com.googlesource.gerrit.plugins.chatgpt;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.client.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.OpenAiClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class PatchSetReviewer {

    private static final String SPLIT_REVIEW_MSG = "Too many changes. Please consider splitting into patches smaller than %s lines for review.";

    private static final int COMMENT_BATCH_SIZE = 25;

    private final Configuration configuration;
    private final GerritClient gerritClient;
    private final OpenAiClient openAiClient;

    @Inject
    public PatchSetReviewer(Configuration configuration, GerritClient gerritClient, OpenAiClient openAiClient) {
        this.configuration = configuration;
        this.gerritClient = gerritClient;
        this.openAiClient = openAiClient;
    }

    public void review(String changeId) throws IOException, InterruptedException {
        log.info("Starting to review patch set: changeId={}", changeId);

        String patchSet = gerritClient.getPatchSet(changeId);
        if (configuration.isPatchSetReduction()) {
            patchSet = reducePatchSet(patchSet);
            log.debug("Reduced patch set: {}", patchSet);
        }

        String reviewSuggestion = getReviewSuggestion(changeId, patchSet);
        List<String> reviewBatches = splitReviewIntoBatches(reviewSuggestion);

        for (String reviewBatch : reviewBatches) {
            gerritClient.postComment(changeId, reviewBatch);
            log.debug("Posted review batch: {}", reviewBatch);
        }

        log.info("Finished reviewing patch set: changeId={}", changeId);

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

    private String reducePatchSet(String patchSet) {
        Set<String> skipPrefixes = new HashSet<>(Arrays.asList(
                "import", "-", "+package", "+import", "From", "Date:", "Subject:",
                "Change-Id:", "diff --git", "index", "---", "+++", "@@", "Binary files differ"
        ));

        return Arrays.stream(patchSet.split("\n"))
                .map(line -> line.replace("\t", "").replace("    ", ""))
                .filter(line -> skipPrefixes.stream().noneMatch(line::startsWith))
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.joining("\n"));
    }

    private String getReviewSuggestion(String changeId, String patchSet) throws IOException, InterruptedException {
        log.info("Starting review for changeId: {}", changeId);

        List<String> patchLines = Arrays.asList(patchSet.split("\n"));
        if (patchLines.size() > configuration.getMaxReviewLines()) {
            log.warn("Patch set too large. Skipping review. changeId: {}", changeId);
            return String.format(SPLIT_REVIEW_MSG, configuration.getMaxReviewLines());
        }

        String reviewSuggestion = openAiClient.ask(patchSet);
        log.info("Review completed for changeId: {}", changeId);
        return reviewSuggestion;

    }
}

