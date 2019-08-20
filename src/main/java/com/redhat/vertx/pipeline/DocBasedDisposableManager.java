package com.redhat.vertx.pipeline;

import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;

import java.util.*;

public abstract class DocBasedDisposableManager implements Step {
    private Map<String, Collection<Disposable>> disposables = new HashMap<>();


    protected void addDisposable(String uuid, Disposable disposable) {
        this.addDisposable(uuid,Arrays.asList(disposable));
    }

    protected void addDisposable(String uuid, Collection disposables) {
        Collection d = this.disposables.getOrDefault(uuid,new ArrayList<>());
        this.disposables.put(uuid,d);
        d.addAll(disposables);
    }

    @Override
    public void finish(String uuid) {
        Collection<Disposable> doomed = disposables.remove(uuid);
        if (doomed != null) {
            doomed.iterator().forEachRemaining(Disposable::dispose);
        }
    }

}
