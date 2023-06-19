package com.googlesource.gerrit.plugins.chatgpt;

import com.google.common.base.Splitter;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PatchSetCreated implements EventListener {

    private final Configuration configuration;

    private final PatchSetReviewer reviewer;

    private final List<String> enabledRepos;

    @Inject
    public PatchSetCreated(Configuration configuration, PatchSetReviewer reviewer) {
        this.configuration = configuration;
        this.reviewer = reviewer;
        this.enabledRepos = Splitter.on(",").omitEmptyStrings().splitToList(configuration.getEnabledRepos());
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof PatchSetCreatedEvent)) {
            log.debug("The event is not a PatchSetCreatedEvent, it is: {}", event);
            return;
        }

        log.info("Processing event: {}", event);
        log.debug("The configuration is: {}", configuration);

        PatchSetCreatedEvent createdPatchSetEvent = (PatchSetCreatedEvent) event;
        String projectName = createdPatchSetEvent.getProjectNameKey().get();


        if (!configuration.isGlobalEnable() && !enabledRepos.contains(projectName)) {
            log.info("The project {} is not enabled for review", projectName);
            return;
        }

        String branchName = createdPatchSetEvent.getBranchNameKey().shortName();
        String changeKey = createdPatchSetEvent.getChangeKey().get();

        log.info("Processing patch set for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey);

        String reviewId = String.join("~", projectName, branchName, changeKey);
        // Execute the potentially time-consuming operation asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                reviewer.review(reviewId);
            } catch (Exception e) {
                log.error("Failed to submit review for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

}