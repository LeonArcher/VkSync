package com.streamdata.apps.vksync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.streamdata.apps.vksync.models.User;
import com.streamdata.apps.vksync.service.SyncCallback;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VKSdk.login(this, VKScope.FRIENDS);

        // create list view and apply custom list of friends adapter
        ListView lvFriends = (ListView) findViewById(R.id.friendsList);
        adapter = new FriendAdapter(this, friends);
        lvFriends.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                // User passed Authorization
                Log.d(LOG_TAG, "User passed authorization.");

                // TODO: use SyncService
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

    // TODO: implement callback classes

    private class SyncProgressReceiver implements SyncCallback<Float> {
        @Override
        public void callback(Float result) {
        }
    }

    private class SyncResultReceiver implements SyncCallback<List<User>> {
        @Override
        public void callback(List<User> result) {
        }
    }

    private class SyncErrorReceiver implements SyncCallback<Exception> {
        @Override
        public void callback(Exception result) {
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
}
