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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;


public class MainActivity extends SmartActivity {
    private UserManager mUserManager;
    private User mUser;

    private String mLastSyncTime;
    private User mPausedUser;

    private Fragment mFragments[];
    private TabHost mTabHost;
    private int mTabTagIds[] = {R.string.action_inbox, R.string.action_sent, R.string.action_user};
    private String mTabTags[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserManager = UserManager.getInstance();
        mLastSyncTime = "?";
        initBroadcoastReceiver();

        getActionBar().hide();
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
                //Toast.makeText(MainActivity.this, tabId, Toast.LENGTH_LONG).show();
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

        //Intent updateIntent = new Intent(this, UpdateMessageService.class);
        //startService(updateIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        Fragment fragment = null;
        Intent intent = null;
        switch(item.getItemId()) {
        case R.id.action_inbox:
            fragment = new InboxFragment();
            getActionBar().setTitle(R.string.action_inbox);
            break;
        case R.id.action_sent:
            fragment = new SentFragment();
            getActionBar().setTitle(R.string.action_sent);
            break;
        case R.id.action_compose:
            intent = new Intent(this, ComposeActivity.class);
            break;
        case R.id.action_settings:
            intent = new Intent(this, SettingsActivity.class);
            break;
        case R.id.action_change_password:
            intent = new Intent(this, ChangePasswordActivity.class);
            break;
        case R.id.action_personal_info:
            intent = new Intent(this, PersonalInfoActivity.class);
            break;
        }
        if (fragment != null) {

            mFragment = fragment;
            updateView();
            return true;
        } else if (intent != null) {
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_logout) {
            mUserManager.logoutCurrentUser();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent); 
        } else if (item.getItemId() == R.id.action_sync) {
            intent = new Intent(this, UpdateMessageService.class);
            startService(intent);
        }
*/
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        mUserManager.setMainActivityRunning(false);
        mPausedUser = mUserManager.getCurrentUser();
        super.onPause();
    }

    @Override
    public void onResume() {
        //mUser = mUserManager.getCurrentUser();
        super.onResume();

        //checkUpdate();
        mUserManager.setMainActivityRunning(true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void checkUpdate() {
        User currentUser = mUserManager.getCurrentUser();
        if (currentUser == null) return;

        if (mPausedUser == null || currentUser == null || currentUser != mPausedUser || !mUser.getBriefLastUpdateTime().equals(mLastSyncTime)) {
            //mLastUpdateTime = mUser.getBriefLastUpdateTime();

            //NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            //nm.cancel(0);

            //int nUnconfirmedInboxItem = mUserDataManager.getUnconfirmedInboxItems().size();
            //updateView();
        }
    }

    protected void updateView() {
        /*
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(android.R.id.tabcontent, mFragments[0])
                       .commit();*/
    }

    private void initBroadcoastReceiver() {
        IntentFilter filter = new IntentFilter(Configure.BROADCAST_UPDATE_ACTION);
        filter.addAction(Configure.BROADCAST_CONFIRM_MSG_ACTION);
        filter.addAction(Configure.BROADCAST_SEND_MSG_ACTION);
        UpdateStateReceiver receiver = new UpdateStateReceiver();
        LocalBroadcastManager.getInstance(MyApplication.getAppContext()).registerReceiver(receiver, filter);
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

    private class UpdateStateReceiver extends BroadcastReceiver {
        private UpdateStateReceiver() {}

        public void onReceive(Context context, Intent intent) {
            /*
            if (mUserManager.isMainActivityRunning()) {
                checkUpdate();
                String result = intent.getStringExtra(ConfirmMessageService.RESULT_STRING);
                if (result == null) {
                    result = getString(R.string.last_update_from) + mUser.getBriefLastUpdateTime();
                }
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            } */
        }
    }
}
