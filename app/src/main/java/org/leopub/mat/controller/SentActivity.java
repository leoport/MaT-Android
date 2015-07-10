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
import org.leopub.mat.model.SentItem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class SentActivity extends MessageListActivity<SentItem> {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        SentItem item = mItemList.get(position);
        if (item != null) {
            Intent intent = new Intent(this, SentItemActivity.class);
            intent.putExtra(SentItemActivity.SENT_ITEM_MSG_ID, item.getMsgId());
            startActivity(intent);
        }
    }

    @Override
    protected List<SentItem> getListItems() {
        List<SentItem> items = new ArrayList<>();
        items.addAll(mUser.getSentItems());
        while (items.size() < ITEM_NUM_IN_A_PAGE) {
            items.add(null);
        }
        return items;
    }

    @Override
    protected void buildItemView(View convertView, int position) {
        TextView itemInfoView = (TextView) convertView.findViewById(R.id.item_hint_left);
        TextView itemContentView = (TextView) convertView.findViewById(R.id.item_content);
        SentItem item = mItemList.get(position);
        if (item != null) {
            itemContentView.setText(item.getText());
            itemInfoView.setText(item.getProgress() + "  " + item.getDstTitle());
        } else {
            itemContentView.setText("");
            itemInfoView.setText("");
        }
    }
}
