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

public class SyncMessageService extends IntentService {
    public static final int SYNC_UNKOWN_ERROR  = -1;
    public static final int SYNC_OK      = 0;
    public static final int SYNC_UPDATED = 1;
    public static final int SYNC_NETWORK_ERROR = 2;
    public static final int SYNC_NETWORK_DATA_ERROR = 3;
    public static final int SYNC_AUTH_FAILED = 4;
    public static final String SYNC_RESULT         = "SYNC_RESULT";
    public static final String SYNC_RESULT_HINT    = "SYNC_RESULT_UPDATED";
    private static final String TAG = "SyncMessageService";

    public SyncMessageService() {
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
        int result;

        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() == null)
                throw new NetworkException("No connection available");
            if (user == null) throw new AuthException("No user is loged in");
            updated = user.sync(null);
            isUpdateSuccess = true;
            result = updated ? SYNC_UPDATED : SYNC_OK;
            hint = getString(R.string.last_update_from) + user.getLastSyncTime().toSimpleString();
        } catch (NetworkException e) {
            result = SYNC_NETWORK_ERROR;
            hint = getString(R.string.error_network);
        } catch (NetworkDataException e) {
            result = SYNC_NETWORK_DATA_ERROR;
            hint = getString(R.string.error_network_data);
        } catch (AuthException e) {
            result = SYNC_AUTH_FAILED;
            hint = getString(R.string.error_auth_fail);
        }

        Intent broadcastIntent = new Intent(Configure.BROADCAST_UPDATE_ACTION);
        broadcastIntent.putExtra(SYNC_RESULT, result);
        broadcastIntent.putExtra(SYNC_RESULT_HINT, hint);
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
        Intent i = new Intent(context, SyncMessageService.class);
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
        Intent i = new Intent(context, SyncMessageService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
        pi.cancel();
    }
}
