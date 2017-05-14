package com.streamdata.apps.vksync.service;

import android.os.Binder;
import android.os.Handler;
import android.util.Log;

import com.streamdata.apps.vksync.models.User;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Custom binder with weak connection to service. Allows to launch synchronization task and acquire
 * progress/result/errors via callbacks
 */
public class SyncBinder extends Binder {
    private final WeakReference<SyncService> serviceRef;

    public SyncBinder(SyncService service) {
        this.serviceRef = new WeakReference<>(service);
    }

    public void runSyncProcess(SyncCallback<Float> progressCallback,
                               SyncCallback<List<User>> resultCallback,
                               SyncCallback<Exception> errorCallback,
                               final Handler uiHandler) {

        SyncService service = serviceRef.get();

        if (service == null) {
            Log.e(SyncService.LOG_TAG, "Accessing to dead service.");
            uiHandler.post(new SyncCallbackRunnable<>(errorCallback, new SyncFatalException("Service is not running.")));
            return;
        }

        if (service.executor != null) {
            service.executor.shutdownNow();
        }

        service.executor = Executors.newSingleThreadExecutor();
        service.executor.execute(new SyncTask(progressCallback, resultCallback, errorCallback, uiHandler));
    }
}
