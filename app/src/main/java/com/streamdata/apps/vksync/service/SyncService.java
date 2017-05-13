package com.streamdata.apps.vksync.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.streamdata.apps.vksync.models.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncService extends Service {
    public static final String LOG_TAG = "SyncService";

    private volatile boolean isBound = false;
    private ExecutorService executor = null;

    public SyncService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "Service bound.");

        isBound = true;
        return new Binder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "Service unbound.");

        isBound = false;
        return true; // returning super.onUnbind(intent) will suppress additional calls of onBind and onUnbind
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(LOG_TAG, "Service rebound.");

        isBound = true;
        super.onRebind(intent);
    }

    public class SyncBinder extends Binder {
        public void runSyncProcess(SyncCallback<Float> progressCallback,
                                   SyncCallback<List<User>> resultCallback,
                                   SyncCallback<Exception> errorCallback,
                                   final Handler uiHandler) {
            if (executor != null) {
                executor.shutdownNow();
            }

            executor = Executors.newSingleThreadExecutor();
            executor.execute(new SyncTask(progressCallback, resultCallback, errorCallback, uiHandler));
        }
    }
}
