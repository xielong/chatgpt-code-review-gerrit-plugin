package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.PatchSetReviewer;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class EventListenerHandler {

    private final PatchSetReviewer reviewer;
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
    private final RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("EventListenerHandler-%d")
            .build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, queue, threadFactory, handler);
    private CompletableFuture<Void> latestFuture;

    @Inject
    public EventListenerHandler(PatchSetReviewer reviewer) {
        this.reviewer = reviewer;

        addShutdownHoot();
    }

    public static String buildFullChangeId(Project.NameKey projectName, BranchNameKey branchName, Change.Key changeKey) {
        // project name need escape '/'. Otherwise, the full change id will be invalid.
        // for example: test%2Fproject%2name~test-branch~I7a1f81560359a6688ae3acabe5e753158ddf832a
        String projectNameEscaped = projectName.get().replace("/", "%2F");
        return String.join("~", projectNameEscaped, branchName.shortName(), changeKey.get());
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

    public void handleEvent(Configuration config, ChangeEvent changeEvent) {
        Project.NameKey projectNameKey = changeEvent.getProjectNameKey();
        BranchNameKey branchNameKey = changeEvent.getBranchNameKey();
        Change.Key changeKey = changeEvent.getChangeKey();

        String fullChangeId = buildFullChangeId(projectNameKey, branchNameKey, changeKey);

        List<String> enabledProjects = Splitter.on(",").omitEmptyStrings()
                .splitToList(config.getEnabledProjects());
        if (!config.isGlobalEnable() &&
                !enabledProjects.contains(projectNameKey.get()) &&
                !config.isProjectEnable()) {
            log.info("The project {} is not enabled for review", projectNameKey);
            return;
        }

        // Execute the potentially time-consuming operation asynchronously
        latestFuture = CompletableFuture.runAsync(() -> {
            try {
                log.info("Processing change: {}", fullChangeId);
                reviewer.review(config, fullChangeId);
                log.info("Finished processing change: {}", fullChangeId);
            } catch (Exception e) {
                log.error("Error while processing change: {}", fullChangeId, e);
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
