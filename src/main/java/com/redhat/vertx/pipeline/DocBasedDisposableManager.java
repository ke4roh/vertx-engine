package com.redhat.vertx.pipeline;

import io.reactivex.disposables.Disposable;

import java.util.*;

public class DocBasedDisposableManager {
    private Map<String, Collection<Disposable>> disposablesByDocId = new HashMap<>();


    public void addDisposable(String documentId, Disposable disposable) {
        this.addDisposable(documentId,Collections.singletonList(disposable));
    }

    public void addDisposable(String documentId, Collection<Disposable> disposables) {
        Collection<Disposable> listOfDisposables = this.disposablesByDocId.getOrDefault(documentId,new ArrayList<>());
        this.disposablesByDocId.put(documentId,listOfDisposables);
        listOfDisposables.addAll(disposables);
    }

    public void finish(String documentId) {
        Collection<Disposable> doomedDisposables = disposablesByDocId.remove(documentId);
        if (doomedDisposables != null) {
            doomedDisposables.forEach(Disposable::dispose);
        }
    }

}
