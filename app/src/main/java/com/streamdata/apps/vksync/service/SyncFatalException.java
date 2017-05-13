package com.streamdata.apps.vksync.service;

import com.vk.sdk.api.VKError;

/**
 * TODO: Add a class header comment!
 */
public class SyncFatalException extends Exception {
    private final VKError error;
    private final String message;

    public SyncFatalException(VKError error) {
        this.error = error;
        message = error.toString();
    }

    public SyncFatalException(String message) {
        this.error = null;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
