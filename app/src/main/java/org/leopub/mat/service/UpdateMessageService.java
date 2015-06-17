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

public class UpdateMessageService extends IntentService {
    private static final String TAG = "UpdateMessageService";

    public UpdateMessageService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.i(TAG, "onHandleIntent entered.");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAutoSync = pref.getBoolean("auto_sync", true);
        boolean isUpdateSuccess = false;

        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() == null) throw new NetworkException("No connection available");

            UserManager userManager = UserManager.getInstance();
            User user = userManager.getCurrentUser();
            if (user == null) throw new AuthException("No user is loged in");

            user.sync(null);
            isUpdateSuccess = true;
            Intent broadcastIntent = new Intent(Configure.BROADCAST_UPDATE_ACTION);
            LocalBroadcastManager.getInstance(MyApplication.getAppContext()).sendBroadcast(broadcastIntent);
            if (!userManager.isMainActivityRunning()) {
                List<InboxItem> unconfirmedInboxItems = user.getUnconfirmedInboxItems();
                if (unconfirmedInboxItems.size() > 0) {
                    setNotification(unconfirmedInboxItems);
                }
            }
        } catch (NetworkException e) {
            Logger.i(TAG, e.getMessage());
        } catch (NetworkDataException e) {
            Logger.i(TAG, e.getMessage());
        } catch (AuthException e) {
            Logger.i(TAG, "Auth failed");
        } finally {
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
        Intent i = new Intent(context, UpdateMessageService.class);
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
        Intent i = new Intent(context, UpdateMessageService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
        pi.cancel();
    }
}
