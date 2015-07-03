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

import java.util.ArrayList;
import java.util.List;

import org.leopub.mat.Configure;
import org.leopub.mat.DateTime;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.SentItem;
import org.leopub.mat.service.MessageBroadcastReceiver;
import org.leopub.mat.service.MessageService;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SentActivity extends ListActivity {
    private User mUser;
    private SwipeRefreshLayout mSwipeView;
    List<SentItem> mItemList;
    DateTime mDataTimestamp;
    ArrayAdapter<SentItem> mArrayAdapter;
    private LocalBroadcastManager mBroadcastManager;
    private IntentFilter mBroadcastFilter;
    private MessageBroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refreshable_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mUser = UserManager.getInstance().getCurrentUser();

        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastFilter = new IntentFilter(Configure.BROADCAST_MESSAGE);
        mBroadcastReceiver = new PrivateBroadcastReceiver();

        mItemList = new ArrayList<>();
        mDataTimestamp = mUser.getLastUpdateTime();
        mItemList.addAll(mUser.getSentItems());

        mSwipeView = (SwipeRefreshLayout) findViewById(R.id.swipe);
        mSwipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeView.setRefreshing(true);
                Intent intent = new Intent(SentActivity.this, MessageService.class);
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBroadcastManager.registerReceiver(mBroadcastReceiver, mBroadcastFilter);
        updateView();
    }

    @Override
    public void onPause() {
        mBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, SentItemActivity.class);
        intent.putExtra(SentItemActivity.SENT_ITEM_MSG_ID, mUser.getSentItems().get(position).getMsgId());
        startActivity(intent);
    }

    private void updateView() {
        DateTime now = mUser.getLastUpdateTime();
        if (now.compareTo(mDataTimestamp) != 0) {
            mDataTimestamp = now;
            mItemList.clear();
            mItemList.addAll(mUser.getSentItems());
            mArrayAdapter.notifyDataSetChanged();
        }
    }

    private class PrivateArrayAdapter extends ArrayAdapter<SentItem> {
        public PrivateArrayAdapter(Context context, int resource, int textViewId, List<SentItem> items) {
            super(context, resource, textViewId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, parent, false);
            }
            SentItem item = getItem(position);
            TextView itemContentView = (TextView) convertView.findViewById(R.id.item_content) ;
            itemContentView.setText(item.getContent());

            TextView itemInfoView = (TextView) convertView.findViewById(R.id.item_hint_left); 
            itemInfoView.setText(item.getProgress() + "  " + item.getDstTitle());

            return convertView;
        }
    }

    private class PrivateBroadcastReceiver extends MessageBroadcastReceiver {
        private PrivateBroadcastReceiver() {
            super(SentActivity.this);
        }

        @Override
        public boolean onReceiveEvent(MessageService.Result result, String hint) {
            if (result == MessageService.Result.Updated) {
                updateView();
            } else {
                mSwipeView.setRefreshing(false);
            }
            return false;
        }
    }
}
