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

import org.leopub.mat.Configure;
import org.leopub.mat.HttpUtil;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.model.User;
import org.leopub.mat.model.UserManager;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.service.MessageBroadcastReceiver;
import org.leopub.mat.service.MessageService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private UserManager mUserManager;
    private PrivateBroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);
        mUserManager = UserManager.getInstance();

        mBroadcastReceiver = new PrivateBroadcastReceiver();
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_MESSAGE);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(mBroadcastReceiver, filter);

        User currentUser = mUserManager.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isLogedIn()) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                setUsernameAndPassword(currentUser);
                showLoginProgress(false);
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    public void onLogin(View view) {
        EditText usernameView = (EditText) findViewById(R.id.username);
        String username = usernameView.getText().toString();

        EditText passwordView = (EditText) findViewById(R.id.password);
        String password = passwordView.getText().toString();

        new NetworkTask().execute(username, password);
    }

    public void onSwithUser(View view) {
        List<User> users = mUserManager.getUsers();
        int nUser = users.size();
        String usernames[] = new String[nUser];
        for (int i = 0; i < nUser; i++) {
            usernames[i] = String.valueOf(users.get(i).getUserId());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.switch_user));
        builder.setItems(usernames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                User user = mUserManager.getUsers().get(which);
                mUserManager.setCurrentUser(user);
                setUsernameAndPassword(user);
            }
        });
        builder.create().show();
    }

    private void setUsernameAndPassword(User user) {
        EditText usernameView = (EditText) findViewById(R.id.username);
        usernameView.setText(String.valueOf(user.getUserId()));

        EditText passwordView = (EditText) findViewById(R.id.password);
        passwordView.setText("");
    }
    private void showLoginProgress(boolean showProgress) {
        findViewById(R.id.username).setFocusableInTouchMode(!showProgress);
        findViewById(R.id.password).setFocusableInTouchMode(!showProgress);
        if (showProgress) {
            findViewById(R.id.login_progress).setVisibility(View.VISIBLE);
            findViewById(R.id.login_buttons).setVisibility(View.GONE);
            findViewById(R.id.login_dummy).requestFocus();
        } else {
            findViewById(R.id.login_progress).setVisibility(View.GONE);
            findViewById(R.id.login_buttons).setVisibility(View.VISIBLE);
        }
    }

    private class NetworkTask extends AsyncTask<String, Void, String> {
        @Override
        public void onPreExecute() {
            showLoginProgress(true);
        }

        @Override
        protected String doInBackground(String... args) {
            String username = args[0];
            String password = args[1];
            String result = null;
            try {
                User mUser = new User(MyApplication.getAppContext(), Integer.valueOf(username));
                HttpUtil.auth(mUser, password);
                mUserManager.setCurrentUser(mUser);
                Intent intent = new Intent(LoginActivity.this, MessageService.class);
                startService(intent);
            } catch (NetworkException e) {
                result = getString(R.string.error_network);
            } catch (AuthException e) {
                result = getString(R.string.login_fail);
            }
            return result;
        }

        @Override
        public void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(LoginActivity.this, getString(R.string.initializing), Toast.LENGTH_LONG).show();
            } else {
                showLoginProgress(false);
                Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class PrivateBroadcastReceiver extends MessageBroadcastReceiver {
        public PrivateBroadcastReceiver() {
            super(LoginActivity.this);
        }

        @Override
        public boolean onReceiveEvent(MessageService.Result result, String hint) {
            if (result == MessageService.Result.Updated || result == MessageService.Result.Synchronized) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            } else {
                Toast.makeText(LoginActivity.this, hint, Toast.LENGTH_LONG).show();
            }
            return true;
        }
    }
}