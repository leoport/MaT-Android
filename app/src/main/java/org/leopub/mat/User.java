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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.HintException;
import org.leopub.mat.exception.NetworkDataException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.model.ConfirmItem;
import org.leopub.mat.model.Contact;
import org.leopub.mat.model.InboxItem;
import org.leopub.mat.model.ItemStatus;
import org.leopub.mat.model.SentItem;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

public class User {
    private Context mContext;
    private Configure mConfigure;
    private int mUserId;
    private String mSessionId;
    private String mCookieId;
    private SQLiteDatabase mDatabase = null;
    private Map<String, String> mMajorStringMap;
    private Map<Character, String> mUnitTitleStringMap;
    private List<InboxItem> mInboxItemsCache;
    private List<InboxItem> mUndoneInboxItemsCache;
    private List<SentItem> mSentItemsCache;
    private DateTime mLastUpdateTimestamp;
    private DateTime mLastSyncTimestamp;

    public User(Context context, int userId) {
        mContext = context;
        mConfigure = new Configure(userId);

        mUserId = userId;
        mSessionId = null;
        mCookieId  = null;

        mInboxItemsCache = null;
        mUndoneInboxItemsCache = null;
        mSentItemsCache = null;
        initStringMap();
        initDatabase();
        initTimestamp();
    }
    private void initDatabase() {
        mDatabase = SQLiteDatabase.openOrCreateDatabase(mConfigure.getSQLitePath(), null);
        // create table contact
        String query = "CREATE TABLE IF NOT EXISTS contact (`id` integer PRIMARY KEY, name varchar(255), name_char varchar(10), type char, unit varchar(10), title varchar(10), `f` integer, `b` integer, `t` integer, timestamp timestamp);";
        mDatabase.execSQL(query);
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_timestamp ON contact (timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_unit ON contact (unit);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_name_char ON contact (name_char);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_f ON contact (f);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_b ON contact (b);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_contact_t ON contact (t);");
       // create table inbox 
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS `inbox` (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), param integer, `content` varchar(2048), `status` integer, `timestamp` timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_inbox_timestamp ON inbox (`timestamp`);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_inbox_status ON inbox(`status`);");
         // create table sent
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS sent (`msg_id` integer PRIMARY KEY, `dst_str` varchar(300), `dst_title` varchar(300), parsm integer, `content` varchar(2048), `status` integer, `timestamp` timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_sent_timestamp ON sent (`timestamp`);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_sent_status ON sent (`status`);");
        // create table confirm
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS `confirm` (confirm_id integer PRIMARY KEY, `msg_id` integer, dst_id integer, dst_title varchar(40), `status` integer, timestamp timestamp);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_confirm_timestamp ON confirm(`timestamp`);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_confirm_msg ON confirm(`msg_id`);");
        // create table update_record
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS `sync_record`(`id` integer PRIMARY KEY, timestamp timestamp, length integer, updated integer)");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_record_timestamp ON sync_record(`timestamp`);");
        mDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_record_updated ON sync_record(`updated`);");
    }

    private void initTimestamp() {
        String sql = "SELECT timestamp FROM sync_record WHERE updated=1 ORDER BY timestamp DESC LIMIT 1;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        if (cursor.moveToNext()) {
            mLastUpdateTimestamp = new DateTime(cursor.getString(0));
        } else {
            mLastUpdateTimestamp = new DateTime(0);
        }

        sql = "SELECT timestamp FROM sync_record ORDER BY timestamp DESC LIMIT 1;";
        cursor = mDatabase.rawQuery(sql, null);
        if (cursor.moveToNext()) {
            mLastSyncTimestamp = new DateTime(cursor.getString(0));
        } else {
            mLastSyncTimestamp = new DateTime(0);
        }
        cursor.close();
    }

    private void initStringMap() {
        mMajorStringMap = new HashMap<>();
        mUnitTitleStringMap = new HashMap<>();
        mMajorStringMap.put("cs", mContext.getString(R.string.short_name_of_cs));
        mUnitTitleStringMap.put('a', mContext.getString(R.string.title_for_a));
        mUnitTitleStringMap.put('b', mContext.getString(R.string.title_for_b));
        mUnitTitleStringMap.put('c', mContext.getString(R.string.title_for_c));
        mUnitTitleStringMap.put('h', mContext.getString(R.string.title_for_h));
        mUnitTitleStringMap.put('l', mContext.getString(R.string.title_for_l));
        mUnitTitleStringMap.put('t', mContext.getString(R.string.title_for_t));
        mUnitTitleStringMap.put('w', mContext.getString(R.string.title_for_w));
        mUnitTitleStringMap.put('x', mContext.getString(R.string.title_for_x));
        mUnitTitleStringMap.put('y', mContext.getString(R.string.title_for_y));
        mUnitTitleStringMap.put('z', mContext.getString(R.string.title_for_z));
    }

    public int getUserId() {
        return mUserId;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }

    public String getCookieId() {
        return mCookieId;
    }

    public void setCookieId(String cookieId) {
        mCookieId = cookieId;
    }

    public boolean isLogedIn() {
        return mCookieId != null;
    }

    public boolean sync(String data) throws NetworkException, NetworkDataException, AuthException {
        if (data == null) {
            String since = mLastSyncTimestamp.toDigitString();
            data = HttpUtil.getUrl(this, Configure.MSG_FETCH_URL + "?since=" + since);
        }
        try {
            boolean updated;

            JSONObject jsonObj = new JSONObject(data);

            updated = updateContact(jsonObj.getJSONArray("contact"));

            updated = updateInbox(jsonObj.getJSONArray("inbox")) || updated;
            mInboxItemsCache = null;
            mUndoneInboxItemsCache = null;

            updated = updateConfirm(jsonObj.getJSONArray("confirm")) || updated;

            updated = updateSent(jsonObj.getJSONArray("sent")) || updated;
            mSentItemsCache = null;

            String timestamp = jsonObj.getString("timestamp");
            mLastSyncTimestamp = new DateTime(timestamp);
            if (updated) {
                mLastUpdateTimestamp = mLastSyncTimestamp;
            }
            addUpdateRecord(jsonObj.getString("timestamp"), data.length(), updated);
            return updated;
        } catch (JSONException e) {
            throw new NetworkDataException("Invalid JSON File", e);
        }
    }
    
    public boolean updateContact(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//contact (`id` integer PRIMARY KEY, name varchar(255), type char, unit varchar(10), title varchar(10), timestamp timestamp)
                String query = String.format(
                        "INSERT OR REPLACE INTO contact VALUES('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("name_char"),
                        obj.getString("type"),
                        obj.getString("unit"),
                        obj.getString("title"),
                        obj.getString("f"),
                        obj.getString("b"),
                        obj.getString("t"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
            return n != 0;
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Contact:" + e.getMessage());
        }
    }

   public boolean updateInbox(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//inbox (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), param integer, `content` varchar(2048), `status` integer, `timestamp` timestamp)
                String query = String.format("INSERT OR REPLACE INTO `inbox` VALUES('%s', '%s', '%s', '%s', %s, '%s', '%s');",
                        obj.getString("msg_id"),
                        obj.getString("src_id"),
                        getContactTitle(obj.getString("src_id")),
                        obj.getString("param"),
                        DatabaseUtils.sqlEscapeString(obj.getString("content")),
                        obj.getString("status"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
            return n != 0;
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Inbox:" + e.getMessage());
        }
    }

    public boolean updateSent(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//sent (`msg_id` integer PRIMARY KEY, `dst_str` varchar(300), `dst_title` varchar(300), `content` varchar(2048), `status` integer, `timestamp` timestamp)
                String query = String.format("INSERT OR REPLACE INTO sent VALUES('%s', '%s', '%s', '%s', %s, '%s', '%s');",
                        obj.getString("msg_id"),
                        obj.getString("dst_str"),
                        getGroupsTitle(obj.getString("dst_str")),
                        obj.getString("param"),
                        DatabaseUtils.sqlEscapeString(obj.getString("content")),
                        obj.getString("status"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
            return n != 0;
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Sent:" + e.getMessage());
        }
    }

    public boolean updateConfirm(JSONArray arr) throws NetworkDataException {
        try {
            int n = arr.length();
            for (int i = 0; i < n; i++) {
                JSONObject obj = arr.getJSONObject(i);
//confirm (confirm_id integer PRIMARY KEY, `msg_id` integer, dst_id integer, dst_title varchar(40), `status` integer, timestamp timestamp
                String query = String.format("INSERT OR REPLACE INTO `confirm` VALUES('%s', '%s', '%s', '%s', '%s', '%s');",
                        obj.getString("confirm_id"),
                        obj.getString("msg_id"),
                        obj.getString("dst_id"),
                        getContactTitle(obj.getString("dst_id")),
                        obj.getString("status"),
                        obj.getString("timestamp"));
                Logger.d("SQL", query);
                mDatabase.execSQL(query);
            }
            return n != 0;
        } catch (JSONException e) {
            throw new NetworkDataException("JSON/Confirm:" + e.getMessage());
        }
    }

    public void addUpdateRecord(String timestamp, int length, boolean isDataUpdated) throws NetworkDataException {
        String query = String.format("INSERT INTO sync_record VALUES(NULL, '%s', '%d', '%d');", timestamp, length, isDataUpdated ? 1 : 0);
        Logger.d("SQL", query);
        mDatabase.execSQL(query);
    }

    public DateTime getLastSyncTime() {
        return mLastSyncTimestamp;
    }

    public DateTime getLastUpdateTime() {
        return mLastUpdateTimestamp;
    }

    public List<Contact> getContactsByInitChars(String initChars) {
        List<Contact> res = new ArrayList<>();
        String sql = "SELECT id, name, type, unit, title FROM contact WHERE name_char=? ORDER BY id;";
        String params[] = { initChars };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setId(cursor.getInt(0));
            contact.setName(cursor.getString(1));
            contact.setType(Contact.Type.valueOf(cursor.getString(2)));
            contact.setUnit(cursor.getString(3));
            contact.setTitle(cursor.getString(4));
            res.add(contact);
        }
        cursor.close();
        return res;
    }

    public List<Contact> getUnderling() {
        List<Contact> res = new ArrayList<>();
        Contact me = getContact(mUserId);
        if (me.getTitle().length() == 0) { return res; }
        String sql = "SELECT id, name, type, unit, title FROM contact WHERE `" +  me.getTitle() + "`='" + me.getId() + "'ORDER BY id;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            Contact contact = new Contact();
            contact.setId(cursor.getInt(0));
            contact.setName(cursor.getString(1));
            contact.setType(Contact.Type.valueOf(cursor.getString(2)));
            contact.setUnit(cursor.getString(3));
            contact.setTitle(cursor.getString(4));
            res.add(contact);
        }
        cursor.close();
        return res;
    }

    public Contact getContact(int id) {
        String sql = "SELECT id, name, type, unit, title FROM contact WHERE id=? ORDER BY id;";
        String params[] = { String.valueOf(id) };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        Contact contact = null;
        if (cursor.moveToNext()) {
            contact = new Contact();
            contact.setId(cursor.getInt(0));
            contact.setName(cursor.getString(1));
            contact.setType(Contact.Type.valueOf(cursor.getString(2)));
            contact.setUnit(cursor.getString(3));
            contact.setTitle(cursor.getString(4));
        }
        cursor.close();
        return contact;
    }

    public List<InboxItem> getInboxItems() {
        if (mInboxItemsCache != null) return mInboxItemsCache;

        List<InboxItem> res = new ArrayList<>();
        String sql = "SELECT msg_id, src_id, src_title, content, status, timestamp FROM inbox " +
                     "ORDER BY status ASC, inbox.timestamp DESC;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            InboxItem item = new InboxItem();
            item.setMsgId(cursor.getInt(0));
            item.setSrcId(cursor.getInt(1));
            item.setSrcTitle(cursor.getString(2));
            item.setContent(cursor.getString(3));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(4)));
            item.setTimestamp(new DateTime(cursor.getString(5)));
            res.add(item);
        }
        cursor.close();
        mInboxItemsCache = res;
        return res;
    }

    public InboxItem getInboxItemByMsgId(int msgId) {
        if (mInboxItemsCache != null) {
            for (InboxItem item : mInboxItemsCache) {
                if (item.getMsgId() == msgId) {
                    return item;
                }
            }
        }
        String sql = "SELECT msg_id, src_id, src_title, content, status, timestamp FROM inbox WHERE msg_id=? " +
                     "ORDER BY status ASC, timestamp DESC;";
        String[] params = { String.valueOf(msgId) };
        InboxItem item = new InboxItem();
        Cursor cursor = mDatabase.rawQuery(sql, params);
        if (cursor.moveToNext()) {
            item.setMsgId(cursor.getInt(0));
            item.setSrcId(cursor.getInt(1));
            item.setSrcTitle(cursor.getString(2));
            item.setContent(cursor.getString(3));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(4)));
            item.setTimestamp(new DateTime(cursor.getString(5)));
        }
        cursor.close();
        return item;
    }

    public List<InboxItem> getUndoneInboxItems() {
        if (mUndoneInboxItemsCache != null) return mUndoneInboxItemsCache;

        List<InboxItem> res = new ArrayList<>();
//inbox (`msg_id` integer PRIMARY KEY, `src_id` integer, `src_title` varchar(40), `content` varchar(2048), `status` integer, `timestamp` timestamp)
        String sql = "SELECT msg_id, src_id, src_title, content, status, timestamp FROM inbox WHERE status < 2 " +
                     "ORDER BY timestamp DESC;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            InboxItem item = new InboxItem();
            item.setMsgId(cursor.getInt(0));
            item.setSrcId(cursor.getInt(1));
            item.setSrcTitle(cursor.getString(2));
            item.setContent(cursor.getString(3));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(4)));
            item.setTimestamp(new DateTime(cursor.getString(5)));
            res.add(item);
        }
        cursor.close();
        mUndoneInboxItemsCache = res;
        return res;
    }

    public List<SentItem> getSentItems() {
        if (mSentItemsCache != null) return mSentItemsCache;

        List<SentItem> res = new ArrayList<>();
        String sql = "SELECT msg_id, dst_title, content, status, timestamp FROM sent ORDER BY timestamp DESC;";
        Cursor cursor = mDatabase.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            SentItem item = new SentItem();
            item.setMsgId(cursor.getInt(0));
            item.setDstTitle(cursor.getString(1));
            item.setContent(cursor.getString(2));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(3)));
            item.setTimestamp(new DateTime(cursor.getString(4)));
            item.setProgress(getSentItemProgress(item.getMsgId()));
            res.add(item);
        }
        cursor.close();
        mSentItemsCache = res;
        return res;
    }

    public SentItem getSentItemByMsgId(int msgId) {
        if (mSentItemsCache != null) {
            for (SentItem item : mSentItemsCache) {
                if (item.getMsgId() == msgId) {
                    return item;
                }
            }
        }

        String sql = "SELECT msg_id, dst_title, content, status, timestamp FROM sent WHERE msg_id=? ORDER BY timestamp DESC;";
        String[] params = { String.valueOf(msgId) };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        SentItem item = null;
        if (cursor.moveToNext()) {
            item = new SentItem();
            item.setMsgId(cursor.getInt(0));
            item.setDstTitle(cursor.getString(1));
            item.setContent(cursor.getString(2));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(3)));
            item.setTimestamp(new DateTime(cursor.getString(4)));
            item.setProgress(getSentItemProgress(item.getMsgId()));
        }
        cursor.close();
        return item;
    }

    public List<ConfirmItem> getConfirmItems(int msgId) {
        List<ConfirmItem> res = new ArrayList<>();
        String sql = "SELECT confirm_id, dst_id, dst_title, status, timestamp FROM confirm "
                   + "WHERE msg_id=? ORDER BY timestamp ASC;";
        String[] params = { String.valueOf(msgId) };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        while (cursor.moveToNext()) {
            ConfirmItem item = new ConfirmItem();
            item.setId(cursor.getInt(0));
            item.setMsgId(msgId);
            item.setDstId(cursor.getInt(1));
            item.setDstTitle(cursor.getString(2));
            item.setStatus(ItemStatus.fromOrdial(cursor.getInt(3)));
            item.setTimestamp(new DateTime(cursor.getString(4)));
            res.add(item);
        }
        cursor.close();
        return res;
    }

    public String getSentItemProgress(int msgId) {
        String sql = "SELECT status, COUNT(*) FROM confirm WHERE msg_id=? GROUP BY status;";
        String[] params = { String.valueOf(msgId) };
        Cursor cursor = mDatabase.rawQuery(sql, params);
        int all = 0;
        int confirmed = 0;
        while (cursor.moveToNext()) {
            if (cursor.getInt(0) > 0) {
                confirmed += cursor.getInt(1);
            }
            all += cursor.getInt(1);
        }
        cursor.close();
        return confirmed + "/" + all;
    }

    public String getContactTitle(String id) {
        String params[] = { id };
        Cursor cursor = mDatabase.rawQuery("SELECT name, type FROM contact WHERE id=?;", params);
        String res = null;
        if (cursor.moveToNext()) {
            String name = cursor.getString(0);
            String type = cursor.getString(1);
            if (type.equals("T")) {
                res = name + mContext.getString(R.string.type_for_U);
            } else if (type.equals("S")) {
                res = name + mContext.getString(R.string.type_for_S);
            }
        }
        cursor.close();
        return res;
    }

    private String getGroupsTitle(String groups) {
        StringBuilder sb = new StringBuilder();
        String groupArray[] = groups.split(";");

        for (String group : groupArray) {
            if (group.isEmpty()) {
                continue;
            }
            if (!group.contains(".")) {
                sb.append(getContactTitle(group));
            } else {
                sb.append(getUnitTitle(group));
            }
            sb.append(";");
        }
        return sb.toString();
    }

    public String getUnitTitle(String expr) {
        String exprArray[] = expr.split("\\.");
        String title = exprArray[0];
        String unit  = exprArray[1];
        String major = unit.substring(0, 2);
        StringBuilder sb = new StringBuilder();
        if (!major.equals("__")) {
            sb.append(mMajorStringMap.get(major));
        }
        String grade = unit.substring(3, 4);
        if (!grade.equals("_")) {
            sb.append("1");
            sb.append(grade);
            sb.append(mContext.getString(R.string.unit_grade));
        }
        String className = unit.substring(5, 6);
        if (!className.equals("_")) {
            sb.append(className);
            sb.append(mContext.getString(R.string.unit_class));
        }
        if (title.length() == 0) {
            sb.append(mContext.getString(R.string.all_students));
        } else {
            sb.append(mUnitTitleStringMap.get(title.charAt(0)));
        }
        int nTitle = title.length();
        for (int i = 1; i < nTitle; i++) {
            sb.append(",");
            sb.append(mUnitTitleStringMap.get(title.charAt(i)));
        }
        return sb.toString();
    }

    public String getCategoryJSON(int id) throws NetworkException, AuthException {
        return HttpUtil.getUrl(this, Configure.INFO_CATEGORY_URL + "?id=" + id);
    }

    public void sendMessage(String dst, String content) throws AuthException, HintException, NetworkException, NetworkDataException {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("since=");
            sb.append(mLastSyncTimestamp.toDigitString());
            sb.append("&dst=");
            sb.append(URLEncoder.encode(dst, "utf-8"));
            sb.append("&content=");
            sb.append(URLEncoder.encode(content, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String response = HttpUtil.postURL(this, Configure.MSG_POST_URL, sb.toString());
        if (response.charAt(0) != '[' && response.charAt(0) != '{') {
            throw new HintException(mContext.getString(R.string.send_msg_fail));
        }
        sync(response);
    }

    public void confirmMessage(int srcId, int msgId, int status) throws AuthException, HintException, NetworkException, NetworkDataException {
        String url = String.format(Configure.MSG_CONFIRM_URL, srcId, msgId, status, mLastSyncTimestamp.toDigitString());
        String response = HttpUtil.getUrl(this, url);
        if (response.charAt(0) != '[' && response.charAt(0) != '{') {
            throw new HintException(mContext.getString(R.string.confirm_msg_fail));
        }
        sync(response);
    }

    public void changePassword(String oldPassword, String newPassword) throws AuthException, HintException, NetworkException {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("old_password=");
            sb.append(URLEncoder.encode(oldPassword, "utf-8"));
            sb.append("&new_password=");
            sb.append(URLEncoder.encode(newPassword, "utf-8"));
            sb.append("&repeat_password=");
            sb.append(URLEncoder.encode(newPassword, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String response = HttpUtil.postURL(this, Configure.CHANGE_PASSWORD_URL, sb.toString());
        if (!response.startsWith("Password Updated!")) {
            throw new HintException(response);
        }
    }

    /*
    private String getLastUpdateTimeDigit() {
        String lastUpdateTime = getLastUpdateTime();
        if (lastUpdateTime == null) {
            lastUpdateTime = "1970-01-01 00:00:01";
        }
        return onlyKeepDigit(lastUpdateTime);
    }
    */
}
