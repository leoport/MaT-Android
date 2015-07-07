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

import org.json.JSONException;
import org.json.JSONObject;
import org.leopub.mat.Configure;
import org.leopub.mat.DateTime;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.model.Contact;
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
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class ComposeActivity extends Activity {
    public enum Type {
        Text,
        Meeting,
        Task;
        private static Type[] allValues = values();
        public static Type fromOrdial(int n) { return allValues[n]; }
    }

    private final static String KEY_RECEIVERS = "receivers";
    private final static String KEY_CONTENT = "content";

    private User mUser;
    private List<Contact> mContactsToChoose;
    private String mReceivers;
    private String mContent;
    private Type mMessageType;
    private LocalBroadcastManager mBroadcastManager;
    private IntentFilter mBroadcastFilter;
    private PrivateBroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mUser = UserManager.getInstance().getCurrentUser();
        mReceivers = "";
        mContent = "";

        EditText toEditText = (EditText) findViewById(R.id.compose_to);
        toEditText.addTextChangedListener(new PrivateTextWatcher());

        String[] types = {getString(R.string.message_type_text), getString(R.string.message_type_meeting), getString(R.string.message_type_task)};
        Spinner spinner = (Spinner) findViewById(R.id.message_type);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new PrivateSpinnerListener());

        int dateTimeViewId[] = {R.id.meeting_start_time, R.id.meeting_end_time, R.id.task_deadline};
        ClickDateTimeListener clickDateTimeListener = new ClickDateTimeListener();
        for (int id : dateTimeViewId) {
            EditText view = (EditText)findViewById(id);
            view.setText(new DateTime().toCompleteString());
            view.setOnTouchListener(clickDateTimeListener);
        }
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
        mContent = savedInstanceState.getString(KEY_CONTENT);
        mReceivers = savedInstanceState.getString(KEY_RECEIVERS);

        try {
            JSONObject obj = new JSONObject(mContent);
            mMessageType = Type.valueOf(obj.getString("type"));
            if (mMessageType == Type.Meeting) {
                ((TextView)findViewById(R.id.meeting_start_time)).setText(obj.getString("meeting_start_time"));
                ((TextView)findViewById(R.id.meeting_end_time)).setText(obj.getString("meeting_end_time"));
                ((TextView)findViewById(R.id.meeting_place)).setText(obj.getString("meeting_place"));
            } else if (mMessageType == Type.Task) {
                ((TextView)findViewById(R.id.task_deadline)).setText(obj.getString("task_deadline"));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putString(KEY_RECEIVERS, mReceivers);
        savedInstanceState.putString(KEY_CONTENT, getContentString());
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onSubmit(View view) {
        StringBuilder to = new StringBuilder();
        String[] receiverArr = mReceivers.split(";");
        for (String receiver : receiverArr) {
            to.append(receiver.split(",")[0]);
            to.append(";");
        }

        EditText contentView = (EditText) findViewById(R.id.message_text);
        //String contentStr = contentView.getText().toString();
        String contentStr = getContentString();

        Intent sendMsgIntent = new Intent(this, MessageService.class);
        sendMsgIntent.putExtra(MessageService.FUNCTION_TYPE, MessageService.Function.Send);
        sendMsgIntent.putExtra(MessageService.SEND_DESTINATION, to.toString());
        sendMsgIntent.putExtra(MessageService.SEND_CONTENT, contentStr);
        startService(sendMsgIntent);
        showSendProgress(true);
    }

    private void showSendProgress(boolean showProgress) {
        findViewById(R.id.compose_to).setFocusableInTouchMode(!showProgress);
        findViewById(R.id.message_text).setFocusableInTouchMode(!showProgress);
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

    private String getContentString() {
        JSONObject obj = new JSONObject();
        Spinner spinner =(Spinner)findViewById(R.id.message_type);
        try {
            obj.put("type", mMessageType.toString());
            obj.put("text", ((EditText)findViewById(R.id.message_text)).getText().toString());
            if (mMessageType == Type.Meeting) {
                obj.put("meeting_start_time", ((EditText)findViewById(R.id.meeting_start_time)).getText().toString());
                obj.put("meeting_end_time", ((EditText)findViewById(R.id.meeting_end_time)).getText().toString());
                obj.put("meeting_place", ((EditText)findViewById(R.id.meeting_place)).getText().toString());
            } else if (mMessageType == Type.Task) {
                obj.put("task_deadline", ((EditText)findViewById(R.id.task_deadline)).getText().toString());
            }
            return obj.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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
            mMessageType = Type.fromOrdial(pos);
            findViewById(R.id.message_meeting).setVisibility(mMessageType == Type.Meeting ? View.VISIBLE : View.GONE);
            findViewById(R.id.message_task).setVisibility(mMessageType == Type.Task ? View.VISIBLE : View.GONE);
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

    private class ClickDateTimeListener implements View.OnTouchListener {
        private EditText mCurrentDateTimeView;
        private DateTime mDateTime;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mCurrentDateTimeView = (EditText) view;
                mDateTime = new DateTime(mCurrentDateTimeView.getText().toString());
                new TimePickerDialog(ComposeActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker picker, int hour, int minute) {
                        mDateTime.setHour(hour);
                        mDateTime.setMinute(minute);
                        mCurrentDateTimeView.setText(mDateTime.toCompleteString());
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
