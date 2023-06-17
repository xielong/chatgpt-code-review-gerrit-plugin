package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        DynamicSet.bind(binder(), EventListener.class).to(PatchSetCreated.class);
    }
}
