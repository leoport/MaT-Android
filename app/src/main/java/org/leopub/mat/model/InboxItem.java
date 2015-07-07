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

package org.leopub.mat.model;


import org.json.JSONException;
import org.json.JSONObject;
import org.leopub.mat.DateTime;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class InboxItem {
    public enum Type {
        Text,
        Meeting,
        Task
    };
    private int mMsgId;
    private int mSrcId;
    private String mSrcTitle;
    private Type mType;
    private String mText;
    private ItemStatus mStatus;
    private DateTime mTimestamp;
    private DateTime mMeetingStartTime;
    private DateTime mMeetingEndTime;
    private String mMeetingPlace;
    private DateTime mTaskDeadline;

    public int getMsgId() {
        return mMsgId;
    }

    public void setMsgId(int mMsgId) {
        this.mMsgId = mMsgId;
    }

    public int getSrcId() {
        return mSrcId;
    }

    public void setSrcId(int srcId) {
        mSrcId = srcId;
    }

    public String getSrcTitle() {
        return mSrcTitle;
    }

    public void setSrcTitle(String srcTitle) {
        mSrcTitle = srcTitle;
    }

    public String getText() {
        return mText;
    }

    public Type getType() {
        return mType;
    }

    public DateTime getMeetingStartTime() {
        return mMeetingStartTime;
    }

    public DateTime getMeetingEndTime() {
        return mMeetingEndTime;
    }

    public String getMeetingPlace() {
        return mMeetingPlace;
    }

    public DateTime getTaskDeadline() {
        return mTaskDeadline;
    }

    public void setContent(String content) {
        try {
            String decoded = URLDecoder.decode(content, "utf-8").replace("&quot;", "\"");
            JSONObject obj = new JSONObject(decoded);
            mType = Type.valueOf(obj.getString("type"));
            mText = obj.getString("text");
            if (mType == Type.Meeting) {
                mMeetingStartTime = new DateTime(obj.getString("meeting_start_time"));
                mMeetingEndTime = new DateTime(obj.getString("meeting_end_time"));
                mMeetingPlace = obj.getString("meeting_place");
            } else if (mType == Type.Task) {
                mTaskDeadline = new DateTime(obj.getString("task_deadline"));
            }
        } catch (JSONException e) {
            mType = Type.Text;
            mText = content;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public ItemStatus getStatus() {
        return mStatus;
    }

    public void setStatus(ItemStatus status) {
        mStatus = status;
    }

    public DateTime getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        mTimestamp = timestamp;
    }
}
