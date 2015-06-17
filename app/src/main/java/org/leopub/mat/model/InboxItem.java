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
    private String mContent;
    private ItemStatus mStatus;
    private String mTimestamp;

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

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        this.mContent = content;
    }

    public ItemStatus getStatus() {
        return mStatus;
    }

    public void setStatus(ItemStatus status) {
        mStatus = status;
    }

    public String getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(String timestamp) {
        mTimestamp = timestamp;
    }
}
