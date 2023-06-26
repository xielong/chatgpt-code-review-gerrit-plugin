package com.googlesource.gerrit.plugins.chatgpt;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class PatchSetCreated implements EventListener {
    private final ConfigCreator configCreator;
    private final PatchSetReviewer reviewer;
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
    private final RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("PatchSetReviewExecutorThread-%d")
            .build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, queue, threadFactory, handler);
    private CompletableFuture<Void> latestFuture;

    @Inject
    PatchSetCreated(ConfigCreator configCreator, PatchSetReviewer reviewer) {
        this.configCreator = configCreator;
        this.reviewer = reviewer;

        addShutdownHoot();

    }

    public static String buildFullChangeId(String projectName, String branchName, String changeKey) {
        return String.join("~", projectName, branchName, changeKey);
    }

    private void addShutdownHoot() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
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
        }, executorService);
    }

    public CompletableFuture<Void> getLatestFuture() {
        return latestFuture;
    }

}