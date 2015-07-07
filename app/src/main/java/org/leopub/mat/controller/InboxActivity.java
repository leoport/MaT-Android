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

import org.leopub.mat.R;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.ItemStatus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class InboxActivity extends MessageListActivity<InboxItem> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        InboxItem item = mItemList.get(position);
        if (item != null) {
            Intent intent = new Intent(this, InboxItemActivity.class);
            int[] params = {item.getMsgId()};
            intent.putExtra(InboxItemActivity.INBOX_ITEM_MSG_ID, params);
            startActivity(intent);
        }
    }

    @Override
    protected List<InboxItem> getListItems() {
        List<InboxItem> items = new ArrayList<>();
        items.addAll(mUser.getInboxItems());
        while (items.size() < ITEM_NUM_IN_A_PAGE) {
            items.add(null);
        }
        return items;
    }

    @Override
    protected void buildItemView(View convertView, int position) {
        TextView contentView = (TextView) convertView.findViewById(R.id.item_content);
        TextView leftHintView = (TextView) convertView.findViewById(R.id.item_hint_left);
        TextView rightHintView = (TextView) convertView.findViewById(R.id.item_hint_right);

        InboxItem item = mItemList.get(position);
        if (item != null) {
            contentView.setText(item.getText());
            leftHintView.setText(item.getSrcTitle() + "  " + item.getTimestamp());
            String rightHint = "";
            if (item.getStatus() == ItemStatus.Init) {
                rightHint = getString(R.string.please_confirm);
            } else if (item.getStatus() == ItemStatus.Ignored) {
                rightHint = getString(R.string.ignored);
            } else if (item.getStatus() == ItemStatus.Accomplished) {
                rightHint = getString(R.string.accomplished);
            }
            rightHintView.setText(rightHint);
        } else {
            contentView.setText("");
            leftHintView.setText("");
            rightHintView.setText("");
        }
    }
}

