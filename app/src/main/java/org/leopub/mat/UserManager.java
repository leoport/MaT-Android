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

package org.leopub.mat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserManager {
    static UserManager sInstance = null;

    Context mContext;
    String mAppHomePath;
    User mCurrentUser;
    SQLiteDatabase mLoginDatabase;
    boolean mIsMainActivityRunning;

    static public UserManager getInstance() {
        if (sInstance == null) {
            sInstance = new UserManager(MyApplication.getAppContext());
        }
        return sInstance;
    }

    private UserManager(Context context) {
        mContext = context;
        mAppHomePath = context.getExternalFilesDir(null).toString();

        mIsMainActivityRunning = false;

        File loginSQLiteFile = new File(mAppHomePath, Configure.LOGIN_SQLITE_FILENAME);
        mLoginDatabase = SQLiteDatabase.openOrCreateDatabase(loginSQLiteFile, null);
        String query = "CREATE TABLE IF NOT EXISTS login("
                     + "username varchar(11) PRIMARY KEY,"
                     + "cookie_id varchar(64),"
                     + "last_login timestamp);";
        mLoginDatabase.execSQL(query);

        String columns[] = { "username, cookie_id" };
        Cursor cursor = mLoginDatabase.query(
                "login", columns, null, null, null, null, "last_login DESC", "0, 1");

        mCurrentUser = null;

        if (cursor.moveToNext()) {
            mCurrentUser = new User(context, cursor.getString(0));
            mCurrentUser.setCookieId(cursor.getString(1));
        }
    }

    public User getCurrentUser() {
        return mCurrentUser;
    }

    public void setCurrentUser(User user) {
        mCurrentUser = user;
        if (user != null) {
            String sql = String.format("INSERT OR REPLACE INTO login VALUES('%s', '%s', CURRENT_TIMESTAMP);",
                    user.getUsername(), user.getCookieId());
            mLoginDatabase.execSQL(sql);
        }
    }

    public void logoutCurrentUser() {
        if (mCurrentUser != null) {
            mCurrentUser.setCookieId(null);
            mCurrentUser.setSessionId(null);
            String sql = String.format("UPDATE login SET cookie_id = NULL WHERE username = '%s';", mCurrentUser.getUsername());
            mLoginDatabase.execSQL(sql);
        }
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<User>();
        String columns[] = { "username, cookie_id" };
        Cursor cursor = mLoginDatabase.query(
                "login", columns, null, null, null, null, "last_login DESC", null);

        while (cursor.moveToNext()) {
            User user = new User(mContext, cursor.getString(0));
            user.setCookieId(cursor.getString(1));
            users.add(user);
        }
        return users;
    }

    public boolean isMainActivityRunning() {
        return mIsMainActivityRunning;
    }

    public void setMainActivityRunning(boolean isMainActivityRunning) {
        mIsMainActivityRunning = isMainActivityRunning;
    }
    protected void finalize() {
        mLoginDatabase.close();
    }
}