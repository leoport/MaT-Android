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
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.ItemStatus;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class InboxActivity extends MessageListActivity<InboxItem> {
    @Override
    protected List<InboxItem> getListItems() {
        return mUser.getInboxItems();
    }

    @Override
    protected void buildItemView(View convertView, int position) {
        InboxItem item = mItemList.get(position);
        TextView contentView = (TextView) convertView.findViewById(R.id.item_content);
        contentView.setText(item.getContent());

        TextView leftHintView = (TextView) convertView.findViewById(R.id.item_hint_left);
        leftHintView.setText(item.getSrcTitle() + "  " + item.getTimestamp());

        String rightHint = "";
        if (item.getStatus() == ItemStatus.Init) {
            rightHint = getString(R.string.please_confirm);
        }
        TextView rightHintView = (TextView) convertView.findViewById(R.id.item_hint_right);
        rightHintView.setText(rightHint);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, InboxItemActivity.class);
        int[] params = {mUser.getInboxItems().get(position).getMsgId()};
        intent.putExtra(InboxItemActivity.INBOX_ITEM_MSG_ID, params);
        startActivity(intent);
    }
}

