package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PatchSetCreatedListener implements EventListener {
    private final ConfigCreator configCreator;

    private final EventListenerHandler eventListenerHandler;

    @Inject
    public PatchSetCreatedListener(ConfigCreator configCreator, EventListenerHandler eventListenerHandler) {
        this.configCreator = configCreator;
        this.eventListenerHandler = eventListenerHandler;
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof PatchSetCreatedEvent)) {
            log.debug("The event is not a PatchSetCreatedEvent, it is: {}", event);
            return;
        }

        log.info("Processing event: {}", event);
        ChangeEvent changeEvent = (ChangeEvent) event;
        Project.NameKey projectNameKey = changeEvent.getProjectNameKey();

        try {
            Configuration config = configCreator.createConfig(projectNameKey);
            eventListenerHandler.handleEvent(config, changeEvent);
        } catch (NoSuchProjectException e) {
            log.error("Project not found: {}", projectNameKey, e);
        }
    }


}