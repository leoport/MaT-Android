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
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.HintException;
import org.leopub.mat.exception.NetworkException;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ChangePasswordActivity extends Activity {
    private UserManager mUserManager;
    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_change_password);
        mUserManager = UserManager.getInstance();
        mUser = UserManager.getInstance().getCurrentUser();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onChangePassword(View view) {
        EditText editText = (EditText) findViewById(R.id.old_password);
        String oldPassword = editText.getText().toString();

        editText = (EditText) findViewById(R.id.new_password);
        String newPassword = editText.getText().toString();

        editText = (EditText) findViewById(R.id.repeat_password);
        String repeatPassword = editText.getText().toString();

        if (newPassword.equals(repeatPassword)) {
            new NetworkTask().execute(oldPassword, newPassword);
        } else {
            Toast.makeText(this, getString(R.string.repeat_password_unmatch), Toast.LENGTH_LONG).show();
        }

    }

    private void showProgress(boolean showProgress) {
        findViewById(R.id.old_password).setFocusableInTouchMode(!showProgress);
        findViewById(R.id.new_password).setFocusableInTouchMode(!showProgress);
        findViewById(R.id.repeat_password).setFocusableInTouchMode(!showProgress);
        if (showProgress) {
            findViewById(R.id.change_password_progress).setVisibility(View.VISIBLE);
            findViewById(R.id.change_password_submit).setVisibility(View.GONE);
            findViewById(R.id.dummy).requestFocus();
        } else {
            findViewById(R.id.change_password_progress).setVisibility(View.GONE);
            findViewById(R.id.change_password_submit).setVisibility(View.VISIBLE);
        }
    }

    private class NetworkTask extends AsyncTask<String, Void, String> {
        @Override
        public void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected String doInBackground(String... args) {
            String oldPassword = args[0];
            String newPassword = args[1];
            String result = null;
            try {
                mUser.changePassword(oldPassword, newPassword);
            } catch (NetworkException e) {
                result = getString(R.string.error_network);
            } catch (AuthException e) {
                result = getString(R.string.login_fail);
            } catch (HintException e) {
                result = e.getMessage();
            }
            return result;
        }

        @Override
        public void onPostExecute(String result) {
            if (result == null) {
                Toast.makeText(ChangePasswordActivity.this, getString(R.string.change_password_success), Toast.LENGTH_LONG).show();
                mUserManager.logoutCurrentUser();
                finish();
            } else {
                showProgress(false);
                Toast.makeText(ChangePasswordActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }
}
