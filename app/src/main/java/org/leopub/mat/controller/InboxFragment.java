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

import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.ItemStatus;
import org.leopub.mat.service.SyncMessageService;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class InboxFragment extends ListFragment {
    private Context mContext;
    private UserManager mUserManager;
    private User mUser;
    private SwipeRefreshLayout mSwipeView;
    List<InboxItem> mInboxItemList;
    ArrayAdapter<InboxItem> mArrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = MyApplication.getAppContext();
        mUserManager = UserManager.getInstance();
        mInboxItemList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstaceState) {
        return inflater.inflate(R.layout.fragment_list, parent, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View rootView = getView();
        mSwipeView = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe);
        mSwipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeView.setRefreshing(true);
                Intent intent = new Intent(getActivity(), SyncMessageService.class);
                getActivity().startService(intent);
            }
        });
        mArrayAdapter = new InboxArrayAdapter(mContext, R.layout.list_item, R.id.item_content, mInboxItemList);
        ListView listView = getListView();
        listView.setAdapter(mArrayAdapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) { }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                mSwipeView.setEnabled(firstVisibleItem == 0);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mUser = mUserManager.getCurrentUser();
        updateView();
        if (mUser != null){
            Toast.makeText(mContext, getString(R.string.last_update_from) + mUser.getLastSyncTime().toSimpleString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        mSwipeView.setRefreshing(false);
        mSwipeView.destroyDrawingCache();
        mSwipeView.clearAnimation();
        super.onPause();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(mContext, InboxItemActivity.class);
        int[] params = { mUser.getInboxItems().get(position).getMsgId() };
        intent.putExtra(InboxItemActivity.INBOX_ITEM_MSG_ID, params);
        startActivity(intent);
    }

    public void notifySyncEvent() {
        if (isResumed()) {
            updateView();
            mSwipeView.setRefreshing(false);
        }
    }

    private void updateView() {
        mInboxItemList.clear();
        if (mUser != null) {
            mInboxItemList.addAll(mUser.getInboxItems());
        }
        mArrayAdapter.notifyDataSetChanged();
    }

    private class InboxArrayAdapter extends ArrayAdapter<InboxItem> {
        public InboxArrayAdapter(Context context, int resource, int textViewId, List<InboxItem> items) {
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
}
