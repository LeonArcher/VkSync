package com.streamdata.apps.vksync.service;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.streamdata.apps.vksync.models.User;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.model.VKUsersArray;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TODO: Add a class header comment!
 */
class SyncTask implements Runnable {
    private final SyncCallback<String> progressCallback;
    private final SyncCallback<List<User>> resultCallback;
    private final SyncCallback<Exception> errorCallback;
    private final Handler uiHandler;

    public SyncTask(SyncCallback<String> progressCallback,
                    SyncCallback<List<User>> resultCallback,
                    SyncCallback<Exception> errorCallback,
                    Handler uiHandler) {

        this.errorCallback = errorCallback;
        this.progressCallback = progressCallback;
        this.resultCallback = resultCallback;
        this.uiHandler = uiHandler;
    }

    @Override
    public void run() {
        // Request list of friends
        VKRequest currentRequest = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS, "id,first_name,last_name,contacts,photo_100"));
        currentRequest.attempts = 10;

        currentRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(final VKResponse response) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        parseResponse(response);
                    }
                }).start();
            }

            @Override
            public void onError(VKError error) {
                // Do error stuff
                Log.e(SyncService.LOG_TAG, "Request failed.");
                uiHandler.post(new SyncCallbackRunnable<>(errorCallback, new SyncFatalException(error)));
            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                // I don't really believe in progress
                Log.e(SyncService.LOG_TAG, String.format("Request attempt failed: %d/%d", attemptNumber, totalAttempts));
            }

            @Override
            public void onProgress(VKRequest.VKProgressType progressType, long bytesLoaded, long bytesTotal) {
                uiHandler.post(new SyncCallbackRunnable<>(
                        progressCallback,
                        String.format(Locale.US, "Downloading friends: %d%", 100 * bytesLoaded / bytesTotal)
                ));
            }
        });
    }

    @WorkerThread
    private void parseResponse(VKResponse response) {
        // Do complete stuff
        Log.d(SyncService.LOG_TAG, "Request completed.");

        List<User> friends = new ArrayList<>();

        // Parsing friends' data
        VKUsersArray friendsArray = (VKUsersArray) response.parsedModel;

        for (int idx = 0; idx < friendsArray.size(); idx++) {

            uiHandler.post(new SyncCallbackRunnable<>(
                    progressCallback,
                    String.format(Locale.US, "Parsing friends: %d of %d", idx, friendsArray.size())
            ));

            VKApiUserFull friendFull = friendsArray.get(idx);

            Bitmap photo = null;
            try {
                URL url = new URL(friendFull.photo_100);
                photo = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch(IOException ex) {
                Log.e(SyncService.LOG_TAG, String.format("Unable to load user photo: %s", friendFull.photo_100));
                uiHandler.post(new SyncCallbackRunnable<>(errorCallback, ex));
            }

            User friendFullParsed = new User(
                    friendFull.first_name,
                    friendFull.last_name,
                    friendFull.mobile_phone,
                    photo
            );
            friends.add(friendFullParsed);

            Log.d(SyncService.LOG_TAG, friendFullParsed.toString());
        }

        // TODO: handle friends into phone book
        uiHandler.post(new SyncCallbackRunnable<>(resultCallback, friends));
    }
}
