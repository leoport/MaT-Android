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

import org.leopub.mat.R;
import org.leopub.mat.model.UserManager;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.MessageStatus;
import org.leopub.mat.model.MessageType;
import org.leopub.mat.service.MessageService;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends MessageListActivity<InboxItem> {
    private UserManager mUserManager;

    @Override
    protected List<InboxItem> getListItems() {
        List<InboxItem> items = new ArrayList<>();
        items.addAll(mUser.getUndoneInboxItems());
        while (items.size() < ITEM_NUM_IN_A_PAGE) {
            items.add(null);
        }
        return items;
    }

    @Override
    protected void buildItemView(View convertView, int position) {
        TextView contentView = (TextView) convertView.findViewById(R.id.item_content) ;
        TextView leftHintView = (TextView) convertView.findViewById(R.id.item_hint_left);
        TextView rightHintView = (TextView) convertView.findViewById(R.id.item_hint_right);
        ImageView iconView = (ImageView) convertView.findViewById(R.id.item_icon);

        InboxItem item = mItemList.get(position);
        if (item != null) {
            contentView.setText(item.getText());
            String leftHint = "";
            String rightHint = "";
            int iconId;
            if (item.getStatus() == MessageStatus.Init) {
                rightHintView.setTextColor(0xffff0000);
                leftHint = item.getSrcTitle();
                rightHint = getString(R.string.please_confirm);
                iconId = R.drawable.message_init;
            } else {
                rightHintView.setTextColor(0xff000000);
                if (item.getType() == MessageType.Event) {
                    rightHint = item.getStartTime().toSimpleString() + getString(R.string.start);
                    iconId = R.drawable.message_event;
                } else if (item.getType() == MessageType.Task){
                    rightHint = getString(R.string.task_close_time) + item.getEndTime().toSimpleString();
                    iconId = R.drawable.message_task;
                } else {
                    iconId = R.drawable.message_text;
                }
                leftHint = item.getPlace();
            }
            iconView.setImageResource(iconId);
            leftHintView.setText(leftHint);
            rightHintView.setText(rightHint);
        } else {
            iconView.setImageResource(0);
            contentView.setText("");
            leftHintView.setText("");
            rightHintView.setText("");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManager = UserManager.getInstance();
        startService(new Intent(this, MessageService.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        mUserManager.setMainActivityRunning(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(0);
    }

    @Override
    public void onPause() {
        mUserManager.setMainActivityRunning(false);
        super.onPause();
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
            case R.id.action_calendar:
                clazz = CalendarActivity.class;
                break;
            case R.id.action_personal_info:
                clazz = PersonalInfoActivity.class;
                break;
            case R.id.action_inbox:
                clazz = InboxActivity.class;
                break;
            case R.id.action_sent:
                clazz = SentActivity.class;
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
        InboxItem item = mItemList.get(position);
        if (item != null) {
            Intent intent = new Intent(this, InboxItemActivity.class);
            int[] params = { item.getMsgId() };
            intent.putExtra(InboxItemActivity.INBOX_ITEM_MSG_ID, params);
            startActivity(intent);
        }
    }
}
