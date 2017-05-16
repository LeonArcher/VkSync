package com.streamdata.apps.vksync;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.streamdata.apps.vksync.models.User;
import com.streamdata.apps.vksync.service.SyncBinder;
import com.streamdata.apps.vksync.service.SyncCallback;
import com.streamdata.apps.vksync.service.SyncService;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = "MainActivity";

    private List<User> friends = new ArrayList<>();
    private FriendAdapter adapter;
    private View progressView;
    private TextView progressText;
    private ListView lvFriends;

    private Intent serviceIntent;
    private ServiceConnection serviceConnection;
    private SyncBinder serviceBinder;
    private boolean serviceIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // prepare and start service
        serviceIntent = new Intent(this, SyncService.class);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceBinder = (SyncBinder) service;
                serviceIsBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // called only in extreme situations, so we need to duplicate this in onPause
                serviceIsBound = false;
            }
        };

        startService(serviceIntent);

        VKSdk.login(this, VKScope.FRIENDS);

        // create list view and apply custom list of friends adapter
        lvFriends = (ListView) findViewById(R.id.friends_list);
        adapter = new FriendAdapter(this, friends);
        lvFriends.setAdapter(adapter);

        // prepare progress indication
        progressView = findViewById(R.id.progress_bar);
        progressText = (TextView) findViewById(R.id.progress_text);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                // User passed Authorization
                Log.d(LOG_TAG, "User passed authorization.");

                // TODO: use SyncService
                serviceBinder.runSyncProcess(
                        new SyncProgressReceiver(),
                        new SyncResultReceiver(),
                        new SyncErrorReceiver(),
                        new Handler()
                );

                progressText.setText("Downloading friends...");
                showProgress(true);
            }
            @Override
            public void onError(VKError error) {
                // User didn't pass Authorization
                Log.e(LOG_TAG, "Authorization error.");
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Callback classes for handling events from server
     */

    private class SyncProgressReceiver implements SyncCallback<String> {
        @Override
        public void callback(String result) {
            progressText.setText(result);
        }
    }

    private class SyncResultReceiver implements SyncCallback<List<User>> {
        @Override
        public void callback(List<User> result) {
            showProgress(false);

            friends.clear();
            friends.addAll(result);
            adapter.notifyDataSetChanged();
        }
    }

    private class SyncErrorReceiver implements SyncCallback<Exception> {
        @Override
        public void callback(Exception result) {
            showProgress(false);

            Toast.makeText(
                    getApplicationContext(),
                    "Error occurred. Try again later.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceIsBound) {
            unbindService(serviceConnection);
            serviceIsBound = false;
        }
    }

    /**
     * Custom adapter for better contacts representation (including additional info)
     */
    private static class FriendAdapter extends BaseAdapter {
        private Context context;
        private List<User> friends;

        public FriendAdapter(Context context, List<User> friends) {
            this.context = context;
            this.friends = friends;
        }

        @Override
        public User getItem(int i) {
            return friends.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getCount() {
            return friends.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            User friend = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.friend_list_item, viewGroup, false);

                viewHolder = new ViewHolder(
                        (TextView) convertView.findViewById(R.id.name),
                        (TextView) convertView.findViewById(R.id.mobile_phone),
                        (ImageView) convertView.findViewById(R.id.photo)
                );
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            String mobilePhone = friend.getMobilePhone();
            if (mobilePhone.isEmpty()) {
                mobilePhone = "Not available";
            }

            viewHolder.name.setText(friend.getFullName());
            viewHolder.mobilePhone.setText(mobilePhone);
            viewHolder.photo.setImageBitmap(friend.getPhoto());

            return convertView;
        }

        /**
         * ViewHolder pattern for better list performance
         */
        private static class ViewHolder {
            public final TextView name;
            public final TextView mobilePhone;
            public final ImageView photo;

            public ViewHolder(TextView name, TextView mobilePhone, ImageView photo) {
                this.name = name;
                this.mobilePhone = mobilePhone;
                this.photo = photo;
            }
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            lvFriends.setVisibility(show ? View.GONE : View.VISIBLE);
            lvFriends.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    lvFriends.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

            progressText.setVisibility(show ? View.VISIBLE : View.GONE);
            progressText.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressText.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressText.setVisibility(show ? View.VISIBLE : View.GONE);
            lvFriends.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
