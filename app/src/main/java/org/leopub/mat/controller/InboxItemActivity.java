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
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.ItemStatus;
import org.leopub.mat.service.ConfirmMessageService;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;


public class InboxItemActivity extends Activity {
    public final static String INBOX_ITEM_MSG_ID = "org.leopub.mat.inbox.choosenItemMsgId";
    private InboxItem mItem;
    private ConfirmMsgReceiver mBroadcastReceiver;
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.inbox_item, menu);
        menu.findItem(R.id.action_roger).setVisible(mItem.getStatus() == ItemStatus.Init);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_roger){
            onRoger();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
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
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_CONFIRM_MSG_ACTION);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(mBroadcastReceiver , filter);
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }
    public void onRoger() {
        Intent intent = new Intent(MyApplication.getAppContext(), ConfirmMessageService.class);
        intent.putExtra(ConfirmMessageService.SRC_ID, mItem.getSrcId());
        intent.putExtra(ConfirmMessageService.MSG_ID, mItem.getMsgId());
        startService(intent);
        /*
        mItem.setStatus(ItemStatus.Confirming);
        NetworkTask networkTask = new NetworkTask();
        networkTask.execute();
        */
    }

    /*
    private class NetworkTask extends AsyncTask<String, Void, String> {
        @SuppressLint("DefaultLocale")
        @Override
        protected String doInBackground(String... args) {
            try {
                String url = String.format(Configure.CONFIRM_URL, mItem.getSrcId(), mItem.getMsgId());
                String result = HttpUtil.getUrl(url, mCurrentUser);
                if (result.startsWith("OK")) {
                    mItem.setStatus(ItemStatus.Confirmed);
                    showToastHint(getString(R.string.send_message_OK));
                }
            } catch (NetworkException e) {
                showToastHint(getString(R.string.error_network));
            } catch (AuthException e) {
                showToastHint(getString(R.string.error_auth_fail));
            }
            return "";
        }

        private void showToastHint(final String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    Toast.makeText(InboxItemActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }
        
    } */
    private void updateView() {
        invalidateOptionsMenu();

        String lineSeperator = System.getProperty("line.separator");

        String content = getString(R.string.inbox_item_from) + ": " + mItem.getSrcTitle();
        content += lineSeperator;
        content += getString(R.string.inbox_item_time) + ": " + mItem.getTimestamp();
        content += lineSeperator;
        content += getString(R.string.inbox_item_content) + ": " + mItem.getContent();
        TextView textView = (TextView) findViewById(R.id.inbox_item_content);
        textView.setText(content);
        mBroadcastReceiver = new ConfirmMsgReceiver();
    }

    private class ConfirmMsgReceiver extends BroadcastReceiver {
        private ConfirmMsgReceiver() {}

        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(ConfirmMessageService.RESULT_STRING);
            Toast.makeText(MyApplication.getAppContext(), message, Toast.LENGTH_LONG).show();
            if (getString(R.string.confirm_message_OK).equals(message)) {
                onBackPressed();
            }
        }
    }
}
