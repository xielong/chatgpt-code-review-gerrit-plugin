package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PatchSetCreated implements EventListener {

    private final PatchSetReviewer reviewer;

    @Inject
    public PatchSetCreated(PatchSetReviewer reviewer) {
        this.reviewer = reviewer;
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof PatchSetCreatedEvent)) {
            log.debug("The event is not a PatchSetCreatedEvent, it is: {}", event);
            return;
        }

        log.info("Processing event: {}", event);

        PatchSetCreatedEvent createdPatchSetEvent = (PatchSetCreatedEvent) event;
        String projectName = createdPatchSetEvent.getProjectNameKey().get();
        String branchName = createdPatchSetEvent.getBranchNameKey().shortName();
        String changeKey = createdPatchSetEvent.getChangeKey().get();

        log.info("Processing patch set for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey);

        String reviewId = String.join("~", projectName, branchName, changeKey);
        // Execute the potentially time-consuming operation asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                reviewer.review(reviewId);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Failed to submit review for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey, e);
            } catch (Throwable e) {
                log.error("Failed to submit review for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey, e);
            }
        });
    }

}