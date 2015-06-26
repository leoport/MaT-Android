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
import org.leopub.mat.model.SentItem;
import org.leopub.mat.service.UpdateMessageService;

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

public class SentFragment extends ListFragment {
    private User mUser;
    private Context mContext;
    private SwipeRefreshLayout mSwipeView;
    private boolean mIsDataOutdated = false;
    List<SentItem> mSentItemList;
    ArrayAdapter<SentItem> mArrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = MyApplication.getAppContext();
        mUser = UserManager.getInstance().getCurrentUser();
        mSentItemList = new ArrayList<>();
        if (mUser != null) {
            mSentItemList.addAll(mUser.getSentItems());
        }
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
                Intent intent = new Intent(getActivity(), UpdateMessageService.class);
                getActivity().startService(intent);
            }
        });
        mArrayAdapter = new SentArrayAdapter(mContext, R.layout.list_item, R.id.item_content, mSentItemList);
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
        mIsDataOutdated = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsDataOutdated) {
            updateView();
        }
        if (mUser != null) {
            Toast.makeText(mContext, getString(R.string.last_update_from) + mUser.getBriefLastUpdateTime(), Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(mContext, SentItemActivity.class);
        intent.putExtra(SentItemActivity.SENT_ITEM_MSG_ID, mUser.getSentItems().get(position).getMsgId());
        startActivity(intent);
    }

    public void notifySyncEvent() {
        mUser = UserManager.getInstance().getCurrentUser();
        if (isResumed()) {
            updateView();
            mSwipeView.setRefreshing(false);
        } else {
            mIsDataOutdated = true;
        }
    }

    private void updateView() {
        mSentItemList.clear();
        if (mUser != null) {
            mSentItemList.addAll(mUser.getSentItems());
        }
        mArrayAdapter.notifyDataSetChanged();
        mIsDataOutdated = false;
    }

    private class SentArrayAdapter extends ArrayAdapter<SentItem> {
        public SentArrayAdapter(Context context, int resource, int textViewId, List<SentItem> items) {
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
}
