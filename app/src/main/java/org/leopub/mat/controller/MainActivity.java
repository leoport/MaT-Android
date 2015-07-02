/*
 * Copyright (C) 2015 Liang Jing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.leopub.mat.controller;

import org.leopub.mat.Configure;
import org.leopub.mat.DateTime;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.ItemStatus;
import org.leopub.mat.service.MessageBroadcastReceiver;
import org.leopub.mat.service.MessageService;

import android.app.Activity;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ListActivity {
    private UserManager mUserManager;
    private User mUser;
    private PrivateBroadcastReceiver mBroadcastReceiver;
    private SwipeRefreshLayout mSwipeView;
    List<InboxItem> mItemList;
    ArrayAdapter<InboxItem> mArrayAdapter;
    DateTime mDataTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserManager = UserManager.getInstance();
        mUser = mUserManager.getCurrentUser();

        mItemList = new ArrayList<>();
        mDataTimestamp = mUser.getLastUpdateTime();
        mItemList.addAll(mUser.getUnconfirmedInboxItems());

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        mSwipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeView.setRefreshing(true);
                Intent intent = new Intent(MainActivity.this, MessageService.class);
                intent.putExtra(MessageService.FUNCTION_TYPE, MessageService.Function.Sync);
                startService(intent);
            }
        });

        mArrayAdapter = new PrivateArrayAdapter(this, R.layout.list_item, R.id.item_content, mItemList);
        ListView listView = getListView();
        listView.setAdapter(mArrayAdapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mSwipeView.setEnabled(firstVisibleItem == 0);
            }
        });

        startService(new Intent(this, MessageService.class));
        mBroadcastReceiver = new PrivateBroadcastReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, filter);

        mUserManager.setMainActivityRunning(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(0);

        updateView();
    }

    @Override
    public void onPause() {
        mUserManager.setMainActivityRunning(false);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Class<? extends Activity> clazz = null;
        switch (item.getItemId()) {
            case R.id.action_personal_info:
                clazz = PersonalInfoActivity.class;
                break;
            case R.id.action_compose:
                clazz = ComposeActivity.class;
                break;
            case R.id.action_settings:
                clazz = SettingsActivity.class;
                break;
            case R.id.action_change_password:
                clazz = ChangePasswordActivity.class;
                break;
        }
        if (clazz != null) {
            startActivity(new Intent(this, clazz));
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            mUserManager.logoutCurrentUser();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, InboxItemActivity.class);
        int[] params = { mItemList.get(position).getMsgId() };
        intent.putExtra(InboxItemActivity.INBOX_ITEM_MSG_ID, params);
        startActivity(intent);
    }

    private void updateView() {
        DateTime now = mUser.getLastUpdateTime();
        if (now.compareTo(mDataTimestamp) != 0) {
            mDataTimestamp = now;
            mItemList.clear();
            mItemList.addAll(mUser.getUnconfirmedInboxItems());
            mArrayAdapter.notifyDataSetChanged();
        }
    }

    private class PrivateArrayAdapter extends ArrayAdapter<InboxItem> {
        public PrivateArrayAdapter(Context context, int resource, int textViewId, List<InboxItem> items) {
            super(context, resource, textViewId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, parent, false);
            }
            InboxItem item = getItem(position);
            TextView contentView = (TextView) convertView.findViewById(R.id.item_content) ;
            contentView.setText(item.getContent());

            TextView leftHintView = (TextView) convertView.findViewById(R.id.item_hint_left);
            leftHintView.setText(item.getSrcTitle() + "  " + item.getTimestamp());

            String rightHint = "";
            if (item.getStatus() == ItemStatus.Init) {
                rightHint = getString(R.string.please_confirm);
            }
            TextView rightHintView = (TextView) convertView.findViewById(R.id.item_hint_right);
            rightHintView.setText(rightHint);
            return convertView;
        }
    }

    private class PrivateBroadcastReceiver extends MessageBroadcastReceiver {
        private PrivateBroadcastReceiver() {
            super(MainActivity.this);
        }

        @Override
        public boolean onReceiveEvent(MessageService.Result result, String hint) {
            if (result == MessageService.Result.Updated) {
                updateView();
            }
            mSwipeView.setRefreshing(false);
            return false;
        }
    }
}
