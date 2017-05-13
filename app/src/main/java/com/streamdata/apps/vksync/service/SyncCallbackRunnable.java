package com.streamdata.apps.vksync.service;

/**
 * Proxy class for using SyncCallback as normal Runnable
 */
class SyncCallbackRunnable<T> implements Runnable {

    private final SyncCallback<T> callback;
    private final T result;

    public SyncCallbackRunnable(SyncCallback<T> callback, T result) {
        this.callback = callback;
        this.result = result;
    }

    @Override
    public void run() {
        callback.callback(result);
    }
}
