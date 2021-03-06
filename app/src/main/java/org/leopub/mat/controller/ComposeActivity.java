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
import org.leopub.mat.model.DateTime;
import org.leopub.mat.R;
import org.leopub.mat.model.User;
import org.leopub.mat.model.UserManager;
import org.leopub.mat.model.Contact;
import org.leopub.mat.model.MessageType;
import org.leopub.mat.service.MessageBroadcastReceiver;
import org.leopub.mat.service.MessageService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

public class ComposeActivity extends Activity {
    private final static String KEY_RECEIVERS = "receivers";

    private User mUser;
    private List<Contact> mContactsToChoose;
    private String mReceivers;
    private MessageType mMessageType;
    private DateTime mStartTime;
    private DateTime mEndTime;
    private LocalBroadcastManager mBroadcastManager;
    private IntentFilter mBroadcastFilter;
    private PrivateBroadcastReceiver mBroadcastReceiver;
    private EditText mInputDst;
    private EditText mInputStartTime;
    private EditText mInputEndTime;
    private EditText mInputPlace;
    private EditText mInputText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mUser = UserManager.getInstance().getCurrentUser();
        mReceivers = "";
        mMessageType = MessageType.Text;
        mStartTime = new DateTime();
        mEndTime = new DateTime();

        mInputDst = (EditText) findViewById(R.id.compose_dst);
        mInputStartTime = (EditText) findViewById(R.id.compose_start_time);
        mInputEndTime = (EditText) findViewById(R.id.compose_end_time);
        mInputPlace = (EditText) findViewById(R.id.compose_place);
        mInputText = (EditText) findViewById(R.id.compose_text);

        mInputDst.addTextChangedListener(new PrivateTextWatcher());

        String[] types = {getString(R.string.message_type_text), getString(R.string.message_type_meeting), getString(R.string.message_type_task)};
        Spinner spinner = (Spinner) findViewById(R.id.compose_type);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new PrivateSpinnerListener());

        TouchDateTimeListener touchDateTimeListener = new TouchDateTimeListener();
        mInputStartTime.setText(mStartTime.toSimpleString());
        mInputStartTime.setOnTouchListener(touchDateTimeListener);
        mInputEndTime.setText(mEndTime.toSimpleString());
        mInputEndTime.setOnTouchListener(touchDateTimeListener);
        showSendProgress(false);

        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastFilter = new IntentFilter(Configure.BROADCAST_MESSAGE);
        mBroadcastReceiver = new PrivateBroadcastReceiver();
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
        mBroadcastManager.registerReceiver(mBroadcastReceiver, mBroadcastFilter);
    }

    @Override
    public void onPause() {
        mBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mReceivers = savedInstanceState.getString(KEY_RECEIVERS);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_RECEIVERS, mReceivers);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onSubmit(View view) {
        StringBuilder dst = new StringBuilder();
        String[] receiverArr = mReceivers.split(";");
        for (String receiver : receiverArr) {
            dst.append(receiver.split(",")[0]);
            dst.append(";");
        }

        Intent sendMsgIntent = new Intent(this, MessageService.class);
        sendMsgIntent.putExtra(MessageService.FUNCTION_TYPE, MessageService.Function.Send);
        sendMsgIntent.putExtra(MessageService.SEND_DESTINATION, dst.toString());
        sendMsgIntent.putExtra(MessageService.SEND_TYPE, mMessageType.ordinal());
        sendMsgIntent.putExtra(MessageService.SEND_START_TIME, mStartTime.toCompleteString());
        sendMsgIntent.putExtra(MessageService.SEND_END_TIME, mEndTime.toCompleteString());
        sendMsgIntent.putExtra(MessageService.SEND_PLACE, mInputPlace.getText().toString());
        sendMsgIntent.putExtra(MessageService.SEND_TEXT, mInputText.getText().toString());
        startService(sendMsgIntent);
        showSendProgress(true);
    }

    private void showSendProgress(boolean showProgress) {
        findViewById(R.id.compose_dst).setFocusableInTouchMode(!showProgress);
        findViewById(R.id.compose_text).setFocusableInTouchMode(!showProgress);
        if (showProgress) {
            findViewById(R.id.compose_progress).setVisibility(View.VISIBLE);
            findViewById(R.id.compose_submit).setVisibility(View.GONE);
            findViewById(R.id.compose_dummy).requestFocus();
        } else {
            findViewById(R.id.compose_progress).setVisibility(View.GONE);
            findViewById(R.id.compose_submit).setVisibility(View.VISIBLE);
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
        EditText toView = (EditText) findViewById(R.id.compose_dst);
        toView.setText(mReceivers);
        toView.setSelection(mReceivers.length());
    }

    private void addUnitReceiver(String unitExpr) {
        String str = unitExpr + "," + mUser.getUnitTitle(unitExpr) + ";";
        mReceivers += str;
        EditText toView = (EditText) findViewById(R.id.compose_dst);
        toView.setText(mReceivers);
        toView.setSelection(mReceivers.length());
    }

    private class PrivateTextWatcher implements TextWatcher {
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
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    private class PrivateSpinnerListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            mMessageType = MessageType.fromOrdial(pos);
            findViewById(R.id.compose_time_place).setVisibility(mMessageType != MessageType.Text ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private class PrivateBroadcastReceiver extends MessageBroadcastReceiver {
        private PrivateBroadcastReceiver() {
            super(ComposeActivity.this);
        }

        @Override
        public boolean onReceiveEvent(MessageService.Result result, String hint) {
            if (result == MessageService.Result.Sent) {
                finish();
            } else {
                showSendProgress(false);
            }
            return false;
        }
    }

    private class TouchDateTimeListener implements View.OnTouchListener {
        private EditText mCurrentDateTimeView;
        private DateTime mDateTime;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mCurrentDateTimeView = (EditText) view;
                if (mCurrentDateTimeView == mInputStartTime) {
                    mDateTime = mStartTime;
                } else if (mCurrentDateTimeView == mInputEndTime) {
                    mDateTime = mEndTime;
                } else {
                    return false;
                }
                new TimePickerDialog(ComposeActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker picker, int hour, int minute) {
                        mDateTime.setHour(hour);
                        mDateTime.setMinute(minute);
                        if (mCurrentDateTimeView == mInputStartTime) {
                            mInputStartTime.setText(mStartTime.toSimpleString());
                        } else if (mCurrentDateTimeView == mInputEndTime) {
                            mInputEndTime.setText(mEndTime.toSimpleString());
                        }
                    }
                }, mDateTime.getHour(), mDateTime.getMinute(), true).show();
                new DatePickerDialog(ComposeActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker picker, int year, int month, int day) {
                        mDateTime.setYear(year);
                        mDateTime.setMonth(month);
                        mDateTime.setDay(day);
                        mCurrentDateTimeView.setText(mDateTime.toCompleteString());
                    }
                }, mDateTime.getYear(), mDateTime.getMonth(), mDateTime.getDay()).show();
            }
            return true;
        }
    }
}
