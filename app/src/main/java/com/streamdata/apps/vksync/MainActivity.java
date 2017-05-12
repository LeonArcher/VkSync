package com.streamdata.apps.vksync;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
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

public class MainActivity extends AppCompatActivity {
    public static final String MAIN_LOG_TAG = "MainActivity";

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
                Log.d(MAIN_LOG_TAG, "User passed authorization.");

                // Request list of friends
                VKRequest currentRequest = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS, "id,first_name,last_name,contacts,photo_100"));
                currentRequest.attempts = 10;

                currentRequest.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        // Do complete stuff
                        Log.d(MAIN_LOG_TAG, "Request completed.");

                        // Parsing friends' data
                        friends.clear();
                        VKUsersArray friendsArray = (VKUsersArray) response.parsedModel;

                        for (VKApiUserFull friendFull : friendsArray) {

                            Bitmap photo = null;
                            try {
                                URL url = new URL(friendFull.photo_100);
                                photo = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                            } catch(IOException e) {
                                Log.e(MAIN_LOG_TAG, String.format("Unable to load user photo: %s", friendFull.photo_100));
                            }

                            User friendFullParsed = new User(
                                    friendFull.first_name,
                                    friendFull.last_name,
                                    friendFull.mobile_phone,
                                    photo
                            );
                            friends.add(friendFullParsed);

                            Log.d(MAIN_LOG_TAG, friendFullParsed.toString());
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onError(VKError error) {
                        // Do error stuff
                        Log.e(MAIN_LOG_TAG, "Request failed.");
                    }
                    @Override
                    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                        // I don't really believe in progress
                        Log.e(MAIN_LOG_TAG, String.format("Request attempt failed: %d/%d", attemptNumber, totalAttempts));
                    }
                });
            }
            @Override
            public void onError(VKError error) {
                // User didn't pass Authorization
                Log.e(MAIN_LOG_TAG, "Authorization error.");
            }
        })) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // custom adapter for better contacts representation (including additional info)
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

        // ViewHolder pattern for better list performance
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
