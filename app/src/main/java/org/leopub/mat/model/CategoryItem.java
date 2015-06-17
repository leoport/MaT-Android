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

public class CategoryItem {
    private String mStudentId;
    public CategoryItem(JSONObject jsonObj) throws JSONException {
        mStudentId = jsonObj.getString("student_id");
    }
    public String getStudentId() {
        return mStudentId;
    }
    public void setStudentId(String studentId) {
        mStudentId = studentId;
    }
}
