package com.streamdata.apps.vksync.service;

/**
 * TODO: Add a class header comment!
 */
public interface SyncCallback<T> {
    void callback(T result);
}
