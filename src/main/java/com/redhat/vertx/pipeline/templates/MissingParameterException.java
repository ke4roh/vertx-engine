package com.redhat.vertx.pipeline.templates;

/**
 * A parameter is required from the environment but not found.
 * Thrown by a {@link TemplateProcessor} when things go wrong.
 */
public class MissingParameterException extends RuntimeException {
    public MissingParameterException() { }

    public MissingParameterException(String s) {
        super(s);
    }
}
