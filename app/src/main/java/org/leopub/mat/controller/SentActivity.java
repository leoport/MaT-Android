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
import org.leopub.mat.model.SentItem;

import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class SentActivity extends MessageListActivity<SentItem> {
    @Override
    protected List<SentItem> getListItems() {
        return mUser.getSentItems();
    }

    @Override
    protected void buildItemView(View convertView, int position) {
        SentItem item = mItemList.get(position);
        TextView itemContentView = (TextView) convertView.findViewById(R.id.item_content);
        itemContentView.setText(item.getContent());

        TextView itemInfoView = (TextView) convertView.findViewById(R.id.item_hint_left);
        itemInfoView.setText(item.getProgress() + "  " + item.getDstTitle());
    }
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, SentItemActivity.class);
        intent.putExtra(SentItemActivity.SENT_ITEM_MSG_ID, mUser.getSentItems().get(position).getMsgId());
        startActivity(intent);
    }
}
