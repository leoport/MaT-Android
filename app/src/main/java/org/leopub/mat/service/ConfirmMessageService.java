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

package org.leopub.mat.service;

import org.leopub.mat.Configure;
import org.leopub.mat.Logger;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.HintException;
import org.leopub.mat.exception.NetworkDataException;
import org.leopub.mat.exception.NetworkException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;

public class ConfirmMessageService extends IntentService {
    public static final String RESULT_STRING = "RESULT_STRING";
    public static final String SRC_ID        = "SRC_ID";
    public static final String MSG_ID        = "MSG_ID";

    private static final String TAG          = "ConfirmMessageService";

    public ConfirmMessageService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.i(TAG, "onHandleIntent entered.");
        String result = "";

        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() == null) throw new NetworkException("No connection available");

            UserManager userManager = UserManager.getInstance();
            User user = userManager.getCurrentUser();
            if (user == null) throw new AuthException("No user is loged in");

            int srcId = intent.getIntExtra(SRC_ID, -1);
            int msgId = intent.getIntExtra(MSG_ID, -1);
            user.confirmMessage(srcId, msgId);
            result = getString(R.string.confirm_message_OK);
        } catch (NetworkException e) {
            result = getString(R.string.error_network);
            Logger.i(TAG, e.getMessage());
        } catch (NetworkDataException e) {
            result = getString(R.string.error_network_data);
            Logger.i(TAG, e.getMessage());
        } catch (AuthException e) {
            result = getString(R.string.error_auth_fail);
            Logger.i(TAG, "Auth failed");
        } catch (HintException e) {
            result = e.getMessage();
            Logger.i(TAG, "Auth failed");
        }

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(MyApplication.getAppContext());
        Intent broadcastIntent = new Intent(Configure.BROADCAST_CONFIRM_MSG_ACTION);
        broadcastIntent.putExtra("RESULT_STRING", result);
        broadcastManager.sendBroadcast(broadcastIntent);
    }
}
