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

import java.util.List;

import org.leopub.mat.Configure;
import org.leopub.mat.Logger;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.controller.InboxItemActivity;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.HintException;
import org.leopub.mat.exception.NetworkDataException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.model.InboxItem;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

public class MessageService extends IntentService {
    public enum Function {
        Sync,
        Confirm,
        Send
    }

    public enum Result {
        AuthFailed,
        NetworkDataError,
        NetworkError,
        UnkownError,
        Synchronized,
        Updated,
        Sent,
        Confirmed
    }

    public static final String FUNCTION_TYPE = "FUNCTION_TYPE";
    public static final String SEND_DESTINATION   = "DESTINATION";
    public static final String SEND_CONTENT       = "CONTENT";
    public static final String CONFIRM_SRC_ID        = "SRC_ID";
    public static final String CONFIRM_MSG_ID        = "MSG_ID";
    public static final String RESULT_CODE = "RESULT_CODE";
    public static final String RESULT_HINT = "RESULT_HINT";
    private static final String TAG = "MessageService";

    public MessageService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.i(TAG, "onHandleIntent entered.");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getCurrentUser();
        boolean isAutoSync = pref.getBoolean("auto_sync", true);
        boolean isUpdateSuccess = false;
        boolean updated = false;
        String hint;
        Result result;

        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() == null) {
                throw new NetworkException("No connection available");
            }
            if (user == null) {
                throw new AuthException("No user is loged in");
            }

            Function function = (Function) intent.getSerializableExtra(FUNCTION_TYPE);
            if (function == Function.Send) {
                String dst = intent.getStringExtra(SEND_DESTINATION);
                String content = intent.getStringExtra(SEND_CONTENT);
                user.sendMessage(dst, content);
                result = Result.Sent;
                hint = getString(R.string.send_message_OK);
            } else if (function == Function.Confirm) {
                int srcId = intent.getIntExtra(CONFIRM_SRC_ID, -1);
                int msgId = intent.getIntExtra(CONFIRM_MSG_ID, -1);
                user.confirmMessage(srcId, msgId);
                result = Result.Confirmed;
                hint = getString(R.string.confirm_message_OK);
            } else {
                updated = user.sync(null);
                isUpdateSuccess = true;
                result = updated ? Result.Updated : Result.Synchronized;
                hint = getString(R.string.last_update_from) + user.getLastSyncTime().toSimpleString();
            }
        } catch (NetworkException e) {
            result = Result.NetworkError;
            hint = getString(R.string.error_network);
        } catch (NetworkDataException e) {
            result = Result.NetworkDataError;
            hint = getString(R.string.error_network_data);
        } catch (AuthException e) {
            result = Result.AuthFailed;
            hint = getString(R.string.error_auth_fail);
        } catch (HintException e) {
            result = Result.UnkownError;
            hint = e.getMessage();
        }

        Intent broadcastIntent = new Intent(Configure.BROADCAST_MESSAGE);
        broadcastIntent.putExtra(RESULT_CODE, result);
        broadcastIntent.putExtra(RESULT_HINT, hint);
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).sendBroadcast(broadcastIntent);
        if (!userManager.isMainActivityRunning() && user != null) {
            List<InboxItem> unconfirmedInboxItems = user.getUnconfirmedInboxItems();
            if (unconfirmedInboxItems.size() > 0) {
                setNotification(unconfirmedInboxItems);
            }
        }
        if (isAutoSync) {
            int syncPeriod = 0;
            if (isUpdateSuccess) {
                syncPeriod = Integer.parseInt(pref.getString("auto_sync_period", "240"));
            } else {
                syncPeriod = Integer.parseInt(pref.getString("retry_sync_period", "10"));
            }
            setUpdate(syncPeriod, syncPeriod);
        }
    }

    private void setNotification(List<InboxItem> unconfirmedInboxItems) {
        int nItem = unconfirmedInboxItems.size();
        if (nItem <= 0) return;

        InboxItem firstItem = unconfirmedInboxItems.get(0);
        Intent intent = new Intent(this, InboxItemActivity.class);

        int itemIdArray[] = new int[nItem];
        for (int i = 0; i < nItem; i++) {
            itemIdArray[i] = unconfirmedInboxItems.get(i).getMsgId();
        }
        intent.putExtra(InboxItemActivity.INBOX_ITEM_MSG_ID, itemIdArray);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = String.valueOf(nItem) + getString(R.string.piece_of_unconfirmed_message);
        Notification notification = new NotificationCompat.Builder(this)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(firstItem.getSrcTitle() + ":" + firstItem.getContent())
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    public static void setUpdate(int latency, int period) {
        Context context = MyApplication.getAppContext();
        Logger.i(TAG, "setUpdate latency:" + latency + ", period:" + period);
        Intent i = new Intent(context, MessageService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (period > 0) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + latency * 60 * 1000, period * 60 * 1000, pi);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + latency * 60 * 1000, pi);
        }
        Logger.i(TAG, "setUpdate Done");
    }

    public static void cancelUpdate(Context context) {
        Intent i = new Intent(context, MessageService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
        pi.cancel();
    }
}
