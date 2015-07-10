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


public class InboxItem {
    private int mMsgId;
    private int mSrcId;
    private String mSrcTitle;
    private MessageType mType;
    private MessageStatus mStatus;
    private DateTime mTimestamp;
    private DateTime mStartTime;
    private DateTime mEndTime;
    private String mPlace;
    private String mText;

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

    public MessageType getType() {
        return mType;
    }

    public void setType(MessageType type) {
        mType = type;
    }

    public DateTime getStartTime() {
        return mStartTime;
    }

    public void setStartTime(DateTime mStartTime) {
        this.mStartTime = mStartTime;
    }

    public DateTime getEndTime() {
        return mEndTime;
    }

    public void setEndTime(DateTime mEndTime) {
        this.mEndTime = mEndTime;
    }

    public String getPlace() {
        return mPlace;
    }

    public void setPlace(String mPlace) {
        this.mPlace = mPlace;
    }

    public String getText() {
        return mText;
    }

    public void setText(String mText) {
        this.mText = mText;
    }

    public MessageStatus getStatus() {
        return mStatus;
    }

    public void setStatus(MessageStatus status) {
        mStatus = status;
    }

    public DateTime getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        mTimestamp = timestamp;
    }
}
