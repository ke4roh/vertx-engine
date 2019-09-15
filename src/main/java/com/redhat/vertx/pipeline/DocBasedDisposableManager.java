package com.redhat.vertx.pipeline;

import io.reactivex.disposables.Disposable;

import java.util.*;

public class DocBasedDisposableManager {
    private Map<String, Collection<Disposable>> disposables = new HashMap<>();


    public void addDisposable(String documentId, Disposable disposable) {
        this.addDisposable(documentId,Arrays.asList(disposable));
    }

    public void addDisposable(String documentId, Collection disposables) {
        Collection d = this.disposables.getOrDefault(documentId,new ArrayList<>());
        this.disposables.put(documentId,d);
        d.addAll(disposables);
    }

    public void finish(String documentId) {
        Collection<Disposable> doomed = disposables.remove(documentId);
        if (doomed != null) {
            doomed.iterator().forEachRemaining(Disposable::dispose);
        }
    }

}
