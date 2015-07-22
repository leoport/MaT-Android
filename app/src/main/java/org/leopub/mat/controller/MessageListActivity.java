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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.leopub.mat.Configure;
import org.leopub.mat.model.DateTime;
import org.leopub.mat.R;
import org.leopub.mat.model.User;
import org.leopub.mat.model.UserManager;
import org.leopub.mat.service.MessageBroadcastReceiver;
import org.leopub.mat.service.MessageService;

import java.util.ArrayList;
import java.util.List;


public abstract class MessageListActivity<MessageItem> extends ListActivity {
    public final int ITEM_NUM_IN_A_PAGE = 10;

    protected User mUser;
    private SwipeRefreshLayout mSwipeView;
    List<MessageItem> mItemList;
    ArrayAdapter<MessageItem> mArrayAdapter;
    DateTime mDataTimestamp;
    private LocalBroadcastManager mBroadcastManager;
    private IntentFilter mBroadcastFilter;
    private PrivateBroadcastReceiver mBroadcastReceiver;

    abstract List<MessageItem> getListItems();
    abstract void buildItemView(View convertView, int position);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refreshable_list);

        mUser = UserManager.getInstance().getCurrentUser();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastFilter = new IntentFilter(Configure.BROADCAST_MESSAGE);
        mBroadcastReceiver = new PrivateBroadcastReceiver();

        mItemList = new ArrayList<>();
        mDataTimestamp = mUser.getLastUpdateTime();
        mItemList.addAll(getListItems());

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        mSwipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeView.setRefreshing(true);
                Intent intent = new Intent(MessageListActivity.this, MessageService.class);
                intent.putExtra(MessageService.FUNCTION_TYPE, MessageService.Function.Sync);
                startService(intent);
            }
        });

        mArrayAdapter = new PrivateArrayAdapter(this, R.layout.list_item, R.id.item_content, mItemList);
        ListView listView = getListView();
        listView.setAdapter(mArrayAdapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                View childView = absListView.getChildAt(0);
                int y = (childView == null) ? 0 : childView.getTop();
                mSwipeView.setEnabled(firstVisibleItem == 0 && y == 0);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mBroadcastManager.registerReceiver(mBroadcastReceiver, mBroadcastFilter);
        if (mUser.isLogedIn()) {
            updateView();
        } else {
            finish();
        }
    }

    @Override
    public void onPause() {
        mSwipeView.setRefreshing(false);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateView() {
        DateTime now = mUser.getLastUpdateTime();
        if (now.compareTo(mDataTimestamp) != 0) {
            mDataTimestamp = now;
            mItemList.clear();
            mItemList.addAll(getListItems());
            mArrayAdapter.notifyDataSetChanged();
        }
    }

    private class PrivateArrayAdapter extends ArrayAdapter<MessageItem> {
        public PrivateArrayAdapter(Context context, int resource, int textViewId, List<MessageItem> items) {
            super(context, resource, textViewId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, parent, false);
            }
            buildItemView(convertView, position);
            return convertView;
        }
    }

    private class PrivateBroadcastReceiver extends MessageBroadcastReceiver {
        private PrivateBroadcastReceiver() {
            super(MessageListActivity.this);
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
