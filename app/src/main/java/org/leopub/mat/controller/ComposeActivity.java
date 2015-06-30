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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.leopub.mat.Configure;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.Contact;
import org.leopub.mat.service.MessageService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ComposeActivity extends Activity {
    private final static String KEY_RECEIVERS = "receivers";

    private User mUser;
    private List<Contact> mContactsToChoose;
    private String mReceivers;
    private MessageBroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mUser = UserManager.getInstance().getCurrentUser();
        mReceivers = "";
        if (savedInstanceState != null) {
            mReceivers = savedInstanceState.getString(KEY_RECEIVERS);
        }

        EditText toEditText = (EditText) findViewById(R.id.compose_to);
        toEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.endsWith(" ")) {
                    String[] tokens = str.split(";");
                    String lastToken = tokens[tokens.length - 1].trim();
                    onChooseContact(lastToken);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        showSendProgress(false);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_RECEIVERS, mReceivers);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onSubmit(View view) {
        StringBuilder to = new StringBuilder();
        String[] receiverArr = mReceivers.split(";");
        for (String receiver : receiverArr) {
            to.append(receiver.split(",")[0]);
            to.append(";");
        }

        EditText contentView = (EditText) findViewById(R.id.compose_content);
        String contentStr = contentView.getText().toString();

        Intent sendMsgIntent = new Intent(this, MessageService.class);
        sendMsgIntent.putExtra(MessageService.FUNCTION_TYPE, MessageService.Function.Send);
        sendMsgIntent.putExtra(MessageService.SEND_DESTINATION, to.toString());
        sendMsgIntent.putExtra(MessageService.SEND_CONTENT, contentStr);
        startService(sendMsgIntent);
        showSendProgress(true);
    }

    private void showSendProgress(boolean showProgress) {
        findViewById(R.id.compose_to).setFocusableInTouchMode(!showProgress);
        findViewById(R.id.compose_content).setFocusableInTouchMode(!showProgress);
        if (showProgress) {
            findViewById(R.id.compose_progress).setVisibility(View.VISIBLE);
            findViewById(R.id.compose_send).setVisibility(View.GONE);
            findViewById(R.id.compose_dummy).requestFocus();
        } else {
            findViewById(R.id.compose_progress).setVisibility(View.GONE);
            findViewById(R.id.compose_send).setVisibility(View.VISIBLE);
        }
    }

    public void onChooseContact(String token) {
        Pattern pattern = Pattern.compile(Configure.RE_UNIT);
        Matcher matcher = pattern.matcher(token);
        if (matcher.matches()) {
            addUnitReceiver(token);
            return;
        }

        mContactsToChoose = mUser.getContactsByInitChars(token);
        int nContact = mContactsToChoose.size();
        if (nContact == 1) {
            addSingleReceiver(mContactsToChoose.get(0));
            return;
        }

        String contactNames[] = new String[nContact];
        for (int i = 0; i < nContact; i++) {
            Contact contact = mContactsToChoose.get(i);
            contactNames[i] = contact.getId() + " " + contact.getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_contact));
        builder.setItems(contactNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Contact contact = mContactsToChoose.get(which);
                addSingleReceiver(contact);
            }
        });
        builder.create().show();
    }

    private void addSingleReceiver(Contact contact) {
        String str = contact.getId() + "," + contact.getName() + ";";
        mReceivers += str;
        EditText toView = (EditText) findViewById(R.id.compose_to);
        toView.setText(mReceivers);
        toView.setSelection(mReceivers.length());
    }

    private void addUnitReceiver(String unitExpr) {
        String str = unitExpr + "," + mUser.getUnitTitle(unitExpr) + ";";
        mReceivers += str;
        EditText toView = (EditText) findViewById(R.id.compose_to);
        toView.setText(mReceivers);
        toView.setSelection(mReceivers.length());
    }

    private class MessageBroadcastReceiver extends BroadcastReceiver {
        private MessageBroadcastReceiver() {}

        public void onReceive(Context context, Intent intent) {
            MessageService.Result result = (MessageService.Result)intent.getSerializableExtra(MessageService.RESULT_CODE);
            String hint = intent.getStringExtra(MessageService.RESULT_HINT);
            Toast.makeText(ComposeActivity.this, hint, Toast.LENGTH_LONG).show();
            if (result == MessageService.Result.Sent) {
                onBackPressed();
            } else {
                showSendProgress(false);
            }
        }
    }
}
