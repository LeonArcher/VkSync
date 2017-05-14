package com.streamdata.apps.vksync.service;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
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

import java.io.ByteArrayOutputStream;
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
    private final Context serviceContext;

    public SyncTask(SyncCallback<String> progressCallback,
                    SyncCallback<List<User>> resultCallback,
                    SyncCallback<Exception> errorCallback,
                    Handler uiHandler,
                    Context serviceContext) {

        this.errorCallback = errorCallback;
        this.progressCallback = progressCallback;
        this.resultCallback = resultCallback;
        this.uiHandler = uiHandler;
        this.serviceContext = serviceContext;
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
                        List<User> friends = parseResponse(response);

                        for (int idx = 0; idx < friends.size(); idx++) {
                            User friend = friends.get(idx);

                            uiHandler.post(new SyncCallbackRunnable<>(
                                    progressCallback,
                                    String.format(Locale.US, "Adding friends to phonebook: %d of %d", idx, friends.size())
                            ));
                            addContactToSystemPhonebook(friend);
                        }

                        uiHandler.post(new SyncCallbackRunnable<>(resultCallback, friends));
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
    private List<User> parseResponse(VKResponse response) {
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

            // check if user has a mobile phone number
            String mobilePhone = normalizePhoneNumber(friendFull.mobile_phone);

            if (mobilePhone == null) {
                if (!friendFull.mobile_phone.isEmpty()) {
                    Log.d(SyncService.LOG_TAG, String.format("Invalid mobile phone number: %s", friendFull.mobile_phone));
                }
                continue;
            }

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
                    mobilePhone,
                    photo
            );
            friends.add(friendFullParsed);

            Log.d(SyncService.LOG_TAG, friendFullParsed.toString());
        }

        return friends;
    }

    /**
     * Check if text phone number is valid
     * @param number string candidate for phone number
     * @return modified phone number if number is phone number, null otherwise
     */
    @Nullable private String normalizePhoneNumber(String number) {
        String cleanNumber = number.replaceAll("[^\\d+]", "");

        if (cleanNumber.matches("^[+]?[0-9]{10,12}$")) {
            return cleanNumber;
        }
        return null;
    }

    @WorkerThread
    private void addContactToSystemPhonebook(User user) {

        String displayName = user.getFullName();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        user.getPhoto().compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] photoByteArray = stream.toByteArray();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, user.getMobilePhone())
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoByteArray)
                .build());

        try {
            serviceContext.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception ex) {
            Log.e(SyncService.LOG_TAG, "Contact import error.", ex);
            uiHandler.post(new SyncCallbackRunnable<>(errorCallback, ex));
        }
    }
}
