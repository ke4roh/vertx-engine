package com.redhat.vertx.pipeline;

public interface EventBusMessage {
    String DOCUMENT_STARTED = "documentStarted";
    String DOCUMENT_COMPLETED = "documentCompleted";
    String DOCUMENT_CHANGED = "documentChanged";
    String CHANGE_REQUEST = "changeRequest";

    String SECTION_STARTED = "sectionStarted";
    String SECTION_COMPLETED = "sectionCompleted";
    String SECTION_ERRORED = "sectionErrored";
}
