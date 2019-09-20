package com.redhat.vertx.pipeline;

public class StepDependencyNotMetException extends Exception implements PotentiallyRecoverableException {
    public StepDependencyNotMetException() {
        super();
    }

    public StepDependencyNotMetException(String message) {
        super(message);
    }
}
