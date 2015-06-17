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

public class PersonalInfoItem implements Comparable<PersonalInfoItem> {
    public enum InfoType {
        text,
        select,
        date,
    };
    private String mKey;
    private InfoType mType;
    private String mValue;
    private boolean mIsEditable;
    private String mTitle;
    private String mOptions;
    private String mHint;
    private int mOrder;

    public PersonalInfoItem(JSONObject jsonObj) throws JSONException {
        mKey = jsonObj.getString("key");
        mType = InfoType.valueOf(jsonObj.getString("type"));
        mValue = jsonObj.getString("value");
        mIsEditable = jsonObj.getBoolean("editable");
        mTitle = jsonObj.getString("title");
        mOptions = jsonObj.optString("options");
        mHint = jsonObj.optString("hint");
        mOrder = jsonObj.getInt("order");
    }

    public String getKey() {
        return mKey;
    }
    public void setKey(String key) {
        mKey = key;
    }
    public InfoType getType() {
        return mType;
    }

    public String getHint() {
        return mHint;
    }

    public void setHint(String hint) {
        mHint = hint;
    }

    public void setType(InfoType type) {
        mType = type;
    }

    public String getValue() {
        return mValue;
    }
    public String getTitle() {
        return mTitle;
    }
    public void setTitle(String title) {
        mTitle = title;
    }
    public void setValue(String value) {
        mValue = value;
    }
    public boolean isEditable() {
        return mIsEditable;
    }
    public void setditable(boolean isEditable) {
        mIsEditable = isEditable;
    }
    public String getOptions() {
        return mOptions;
    }
    public void setOptions(String options) {
        mOptions = options;
    }
    public int getOrder() {
        return mOrder;
    }
    public void setOrder(int order) {
        mOrder = order;
    }
    @Override
    public int compareTo(PersonalInfoItem other) {
        if (mOrder == other.mOrder) {
            return 0;
        } else if (mOrder > other.mOrder) {
            return 1;
        } else {
            return -1;
        }
    }
}
