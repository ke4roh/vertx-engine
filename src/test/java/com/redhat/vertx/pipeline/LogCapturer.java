package com.redhat.vertx.pipeline;

import io.vertx.reactivex.MaybeHelper;
import io.vertx.reactivex.core.Vertx;
import rx.Observable;
import rx.observables.StringObservable;

import java.io.*;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class LogCapturer {
    public final Logger log;
    OutputStream logCapturingStream;
    StreamHandler customLogHandler;

    public LogCapturer(Class<? extends Object> clazz) {
        this(Logger.getLogger(clazz.getName()));
    }

    public LogCapturer(Logger log) {
        this.log = log;
    }

    public Observable<String> attachLogCapturer(Vertx vertx) throws IOException {
        PipedInputStream bytesIn = new PipedInputStream();
        logCapturingStream = new PipedOutputStream(bytesIn);
        Handler[] handlers = log.getParent().getHandlers();
        customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
        log.addHandler(customLogHandler);
        BufferedReader logReader = new BufferedReader(new InputStreamReader(bytesIn));
        return StringObservable.split(StringObservable.from(logReader),"$");
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