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

import org.leopub.mat.Configure;
import org.leopub.mat.MyApplication;
import org.leopub.mat.R;
import org.leopub.mat.User;
import org.leopub.mat.UserManager;
import org.leopub.mat.service.ConfirmMessageService;
import org.leopub.mat.service.SyncMessageService;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
    private UserManager mUserManager;
    private User mUser;
    private TabHost mTabHost;
    private Fragment mFragments[];
    private String mTabTags[];
    private int mTabTagIds[] = {R.string.action_inbox, R.string.action_sent, R.string.action_user};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        mUserManager = UserManager.getInstance();
        mUser = mUserManager.getCurrentUser();
        initBroadcoastReceiver();

        setContentView(R.layout.activity_main);

        int nTab = mTabTagIds.length;
        mFragments = new Fragment[nTab];
        mFragments[0] = new InboxFragment();
        mFragments[1] = new SentFragment();
        mFragments[2] = new UserFragment();

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mTabTags = new String[nTab];
        for (int i = 0; i < nTab; i++) {
            mTabTags[i] = getString(mTabTagIds[i]);
            setupTab(new TextView(this), mTabTags[i]);
        }

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                int nTab = mTabTagIds.length;
                for (int i = 0; i < nTab; i++) {
                    if (tabId.equals(mTabTags[i])) {
                        FragmentManager fragmentManager = getFragmentManager();
                        fragmentManager.beginTransaction()
                                .replace(android.R.id.tabcontent, mFragments[i])
                                .commit();
                        break;
                    }
                }
            }
        });
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(android.R.id.tabcontent, mFragments[0])
                .commit();

        Intent updateIntent = new Intent(this, SyncMessageService.class);
        startService(updateIntent);
    }

    @Override
    public void onPause() {
        mUserManager.setMainActivityRunning(false);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mUserManager.setMainActivityRunning(true);
        mUser = mUserManager.getCurrentUser();
        if (mUser == null || !mUser.isLogedIn()){
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(0);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    protected void notifySyncEvent(boolean updated) {
        InboxFragment inboxFragment = (InboxFragment)mFragments[0];
        inboxFragment.notifySyncEvent(updated);

        SentFragment sentFragment = (SentFragment)mFragments[1];
        sentFragment.notifySyncEvent(updated);
    }

    private void setupTab(final View view, final String tag) {
        View tabView = createTabView(this, tag);
        TabHost.TabSpec setContent = mTabHost.newTabSpec(tag).setIndicator(tabView).setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) { return view; }
        });
        mTabHost.addTab(setContent);
    }

    private static View createTabView(final Context context, final String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        return view;
    }

    private void initBroadcoastReceiver() {
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_UPDATE_ACTION);
        filter.addAction(Configure.BROADCAST_CONFIRM_MSG_ACTION);
        filter.addAction(Configure.BROADCAST_SEND_MSG_ACTION);
        UpdateStateReceiver receiver = new UpdateStateReceiver();
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(receiver, filter);
    }

    private class UpdateStateReceiver extends BroadcastReceiver {
        private UpdateStateReceiver() {}

        public void onReceive(Context context, Intent intent) {
            if (mUserManager.isMainActivityRunning()) {
                int result = intent.getIntExtra(SyncMessageService.SYNC_RESULT, SyncMessageService.SYNC_UNKOWN_ERROR);
                switch(result) {
                    case SyncMessageService.SYNC_UPDATED:
                        notifySyncEvent(true);
                        break;
                    default:
                        notifySyncEvent(false);
                }
                String hint =  intent.getStringExtra(SyncMessageService.SYNC_RESULT_HINT);
                Toast.makeText(MainActivity.this, hint, Toast.LENGTH_LONG).show();
            }
        }
    }
}
