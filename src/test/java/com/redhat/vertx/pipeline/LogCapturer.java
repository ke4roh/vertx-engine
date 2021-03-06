package com.redhat.vertx.pipeline;

import java.io.*;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class LogCapturer {
    public final Logger log;
    OutputStream logCapturingStream;
    StreamHandler customLogHandler;

    public LogCapturer(Class<? extends Object> clazz) {
        this(clazz.getName());
    }

    public LogCapturer(Logger log) {
        this.log = log;
    }

    public LogCapturer(String logName) {
        this(Logger.getLogger(logName));
    }

    public void attachLogCapturer() {
        logCapturingStream = new ByteArrayOutputStream();
        Handler[] handlers = log.getParent().getHandlers();
        customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
        log.addHandler(customLogHandler);
    }

    public String getTestCapturedLog() throws IOException {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }
}