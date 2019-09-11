package com.redhat.vertx.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class LogCapturer {
    private final Logger log;

    private OutputStream capturingStream = new ByteArrayOutputStream();
    private StreamHandler customLogHandler;
    private boolean init = false;

    public LogCapturer(Class<? extends Object> clazz) {
        this(Logger.getLogger(clazz.getName()));
    }

    public LogCapturer(Logger log) {
        this.log = log;
    }

    public void setLevel(Level level) {
        this.log.setLevel(level);
        if (init) {
            this.customLogHandler.setLevel(level);
        }
    }

    public Observable<String> attachLogCapturer() throws IOException {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }

                if (!init) {
                    prepLogCapturer();
                }

                while (!subscriber.isUnsubscribed()) {
                    try {
                        String line;
                        capturingStream.flush();
                        line = capturingStream.toString();
                        if (line == null) {
                            break;
                        }
                        Arrays.asList(line.split("$")).forEach(l -> subscriber.onNext(l));

                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }

                if (!subscriber.isUnsubscribed()) {
                    try {
                        subscriber.onCompleted();
                        log.removeHandler(customLogHandler);
                        customLogHandler.close();
                        capturingStream.close();
                    } catch (IOException e) {
                        // I don't care if the close fails
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).publish();
    }

    public String getTestCapturedLog() {
        var buffer = new StringBuffer();

        try {
            this.attachLogCapturer().subscribe(new Subscriber<String>() {
                @Override
                public void onCompleted() {
                    // Nothing, we're done
                }

                @Override
                public void onError(Throwable e) {
                    buffer.append(e.getMessage());
                }

                @Override
                public void onNext(String s) {
                    buffer.append(s);
                }
            });
        } catch (IOException e) {
            return buffer.toString();
        }

        return buffer.toString();
    }

    public void prepLogCapturer() {
        var handlers = log.getParent().getHandlers();
        customLogHandler = new StreamHandler(capturingStream, handlers[0].getFormatter());
        log.addHandler(customLogHandler);
        this.init = true;
    }
}
