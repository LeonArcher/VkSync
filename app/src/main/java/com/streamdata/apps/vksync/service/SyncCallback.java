package com.streamdata.apps.vksync.service;

/**
 * Interface for callback messages from synchronization service
 */
public interface SyncCallback<T> {
    void callback(T result);
}
