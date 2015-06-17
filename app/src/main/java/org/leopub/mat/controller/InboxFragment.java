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

import java.util.List;

import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.ItemStatus;

import android.annotation.SuppressLint;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class InboxFragment extends ListFragment {
    private User mUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstaceState) {
        return inflater.inflate(R.layout.fragment_list, parent, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateView();
    }

    public void updateView() {
        mUser = UserManager.getInstance().getCurrentUser();
        if (mUser == null) return;

        List<InboxItem> inboxItemList = mUser.getInboxItems();
        ArrayAdapter<InboxItem> arrayAdapter = new InboxArrayAdapter(getActivity(), R.layout.list_item, R.id.item_content, inboxItemList);
        TextView textView = new TextView(getActivity());
        textView.setText(getString(R.string.last_update_from) + mUser.getBriefLastUpdateTime());
        textView.setGravity(Gravity.CENTER);
        getListView().addHeaderView(textView);
        getListView().setAdapter(arrayAdapter);
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        if (position == 0) return;
        Intent intent = new Intent(getActivity(), InboxItemActivity.class);
        int[] params = { mUser.getInboxItems().get(position - 1).getMsgId() };
        intent.putExtra(InboxItemActivity.INBOX_ITEM_MSG_ID, params);
        startActivity(intent);
    }

    private class InboxArrayAdapter extends ArrayAdapter<InboxItem> {
        public InboxArrayAdapter(Context context, int resource, int textViewId, List<InboxItem> items) {
            super(context, resource, textViewId, items);
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, null);
            }
            InboxItem item = getItem(position);
            TextView contentView = (TextView) convertView.findViewById(R.id.item_content) ;
            contentView.setText(item.getContent());

            TextView leftHintView = (TextView) convertView.findViewById(R.id.item_hint_left); 
            leftHintView.setText(item.getSrcTitle() + "  " + item.getTimestamp());

            //if (item.getStatus() == ItemStatus.Init) {
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
