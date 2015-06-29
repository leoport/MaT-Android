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


import org.leopub.mat.DateTime;

import java.util.Date;

public class SentItem {
    private int mMsgId;
    private String mDstTitle;
    private String mContent; 
    private ItemStatus mStatus;
    private DateTime mTimestamp;
    private String mProgress;

   public int getMsgId() {
        return mMsgId;
    }

    public void setMsgId(int msgId) {
        mMsgId = msgId;
    }

     public String getDstTitle() {
        return mDstTitle;
    }

    public void setDstTitle(String dstTitle) {
        mDstTitle = dstTitle;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
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

    public String getProgress() {
        return mProgress;
    }

    public void setProgress(String progress) {
        mProgress = progress;
    }
}
