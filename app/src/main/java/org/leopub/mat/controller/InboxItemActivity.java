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

import java.util.Stack;

import org.leopub.mat.Configure;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.model.User;
import org.leopub.mat.model.UserManager;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.MessageStatus;
import org.leopub.mat.model.MessageType;
import org.leopub.mat.service.MessageService;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;


public class InboxItemActivity extends Activity {
    public final static String INBOX_ITEM_MSG_ID = "org.leopub.mat.inbox.choosenItemMsgId";
    private InboxItem mItem;
    private MessageBroadcastReceiver mBroadcastReceiver;
    private User mUser;
    private Stack<Integer> mItemIdStack = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox_item);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mUser = UserManager.getInstance().getCurrentUser();

        Intent intent = getIntent();
        int[] itemIdArray = intent.getIntArrayExtra(INBOX_ITEM_MSG_ID);
        mItemIdStack = new Stack<>();
        for (int itemId : itemIdArray) {
            mItemIdStack.push(itemId);
        }
        mItem = mUser.getInboxItemByMsgId(mItemIdStack.pop());
        updateView();
        mBroadcastReceiver = new MessageBroadcastReceiver();
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_MESSAGE);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.inbox_item, menu);
        menu.findItem(R.id.action_roger).setVisible(mItem.getStatus() == MessageStatus.Init);
        menu.findItem(R.id.action_remind).setVisible(mItem.getStatus() == MessageStatus.Ignored || mItem.getStatus() == MessageStatus.Accomplished);
        menu.findItem(R.id.action_ignore).setVisible(mItem.getStatus() != MessageStatus.Ignored);
        menu.findItem(R.id.action_accomplish).setVisible(mItem.getStatus() != MessageStatus.Accomplished);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MessageStatus messageStatus = MessageStatus.Init;
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_remind:
            case R.id.action_roger:
                messageStatus = MessageStatus.Confirmed;
                break;
            case R.id.action_ignore:
                messageStatus = MessageStatus.Ignored;
                break;
            case R.id.action_accomplish:
                messageStatus = MessageStatus.Accomplished;
                break;
        }
        if (messageStatus != MessageStatus.Init) {
            Intent intent = new Intent(MyApplication.getAppContext(), MessageService.class);
            intent.putExtra(MessageService.FUNCTION_TYPE, MessageService.Function.Confirm);
            intent.putExtra(MessageService.CONFIRM_SRC_ID, mItem.getSrcId());
            intent.putExtra(MessageService.CONFIRM_MSG_ID, mItem.getMsgId());
            intent.putExtra(MessageService.CONFIRM_STATUS, messageStatus.ordinal());
            startService(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mItemIdStack.isEmpty()) {
            finish();
        } else {
            mItem = mUser.getInboxItemByMsgId(mItemIdStack.pop());
            updateView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    private void updateView() {
        invalidateOptionsMenu();

        String lineSeperator = System.getProperty("line.separator");

        String content = getString(R.string.inbox_item_from) + ": " + mItem.getSrcTitle();
        content += lineSeperator;
        content += getString(R.string.inbox_item_post_time) + ": " + mItem.getTimestamp().toSimpleString();
        content += lineSeperator;
        content += lineSeperator;
        content += getString(R.string.inbox_item_content) + ": " + mItem.getText();
        content += lineSeperator;
        if (mItem.getType() != MessageType.Text) {
            content += getString(R.string.start_time) + ":" + mItem.getStartTime().toSimpleString();
            content += lineSeperator;
            content += getString(R.string.end_time) + ":" + mItem.getEndTime().toSimpleString();
            content += lineSeperator;
            content += getString(R.string.place) + ":" + mItem.getPlace();
            content += lineSeperator;
        }
        TextView textView = (TextView) findViewById(R.id.inbox_item_content);
        textView.setText(content);
    }

    private class MessageBroadcastReceiver extends BroadcastReceiver {
        private MessageBroadcastReceiver() {}

        public void onReceive(Context context, Intent intent) {
            MessageService.Result result = (MessageService.Result)intent.getSerializableExtra(MessageService.RESULT_CODE);
            String hint = intent.getStringExtra(MessageService.RESULT_HINT);
            Toast.makeText(MyApplication.getAppContext(), hint, Toast.LENGTH_LONG).show();
            if (result == MessageService.Result.Confirmed) {
                onBackPressed();
            }
        }
    }
}
