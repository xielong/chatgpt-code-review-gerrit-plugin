package com.googlesource.gerrit.plugins.chatgpt;

import com.google.common.base.Splitter;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PatchSetCreated implements EventListener {
    private final ConfigCreator configCreator;
    private final PatchSetReviewer reviewer;
    private CompletableFuture<Void> latestFuture;

    @Inject
    PatchSetCreated(ConfigCreator configCreator, PatchSetReviewer reviewer) {
        this.configCreator = configCreator;
        this.reviewer = reviewer;
    }

    public static String buildFullChangeId(String projectName, String branchName, String changeKey) {
        return String.join("~", projectName, branchName, changeKey);
    }

    @Override
    public void onEvent(Event event) {
        // Execute the potentially time-consuming operation asynchronously
        latestFuture = CompletableFuture.runAsync(() -> {
            if (!(event instanceof PatchSetCreatedEvent)) {
                log.debug("The event is not a PatchSetCreatedEvent, it is: {}", event);
                return;
            }

            log.info("Processing event: {}", event);
            PatchSetCreatedEvent createdPatchSetEvent = (PatchSetCreatedEvent) event;
            Project.NameKey projectNameKey = createdPatchSetEvent.getProjectNameKey();
            String projectName = projectNameKey.get();
            String branchName = createdPatchSetEvent.getBranchNameKey().shortName();
            String changeKey = createdPatchSetEvent.getChangeKey().get();

            log.info("Processing patch set for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey);

            try {
                Configuration config = configCreator.createConfig(projectNameKey);
                List<String> enabledProjects = Splitter.on(",").omitEmptyStrings()
                        .splitToList(config.getEnabledProjects());
                if (!config.isGlobalEnable() &&
                        !enabledProjects.contains(projectName) &&
                        !config.isProjectEnable()) {
                    log.info("The project {} is not enabled for review", projectName);
                    return;
                }

                String fullChangeId = buildFullChangeId(projectName, branchName, changeKey);

                reviewer.review(config, fullChangeId);
                log.info("Review completed for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey);
            } catch (Exception e) {
                log.error("Failed to submit review for project: {}, branch: {}, change key: {}", projectName, branchName, changeKey, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public CompletableFuture<Void> getLatestFuture() {
        return latestFuture;
    }

}