package com.streamdata.apps.vksync.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.ExecutorService;

public class SyncService extends Service {
    public static final String LOG_TAG = "SyncService";

    private volatile boolean isBound = false;
    ExecutorService executor = null;

    public SyncService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "Service bound.");

        isBound = true;
        return new SyncBinder(this);
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
}
