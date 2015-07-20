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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.leopub.mat.model.DateTime;

public class MyApplication extends Application {
    private static Context sContext;

    public void onCreate() {
        super.onCreate();
        MyApplication.sContext = getApplicationContext();
        SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(sContext);
        DateTime.setsFirstDayOfSemester(new DateTime(preferenceManager.getString("first_day_of_semester", "2015-09-07")));
    }

    public static Context getAppContext() {
        return sContext;
    }
}
