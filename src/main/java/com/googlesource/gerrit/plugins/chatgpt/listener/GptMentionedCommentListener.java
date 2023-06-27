package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GptMentionedCommentListener implements EventListener {

    private final ConfigCreator configCreator;

    private final EventListenerHandler eventListenerHandler;

    @Inject
    public GptMentionedCommentListener(ConfigCreator configCreator, EventListenerHandler eventListenerHandler) {
        this.configCreator = configCreator;
        this.eventListenerHandler = eventListenerHandler;
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof CommentAddedEvent)) {
            log.debug("The event is not a CommentAddedEvent, it is: {}", event);
            return;
        }

        log.info("Processing event: {}", event);
        ChangeEvent changeEvent = (ChangeEvent) event;
        Project.NameKey projectNameKey = changeEvent.getProjectNameKey();

        try {
            Configuration config = configCreator.createConfig(projectNameKey);
            String commentText = ((CommentAddedEvent) event).comment;
            if (commentText == null || !commentText.contains("@" + config.getGerritUserName())) {
                log.info("The comment does not mention the bot: {}", commentText);
                return;
            }

            String questionToGpt = commentText.substring(commentText.indexOf("@" + config.getGerritUserName())
                    + config.getGerritUserName().length() + 1);
            config.configureDynamically(Configuration.KEY_GPT_PROMPT, questionToGpt);

            eventListenerHandler.handleEvent(config, changeEvent);
        } catch (NoSuchProjectException e) {
            log.error("Project not found: {}", projectNameKey, e);
        }

    }
}
