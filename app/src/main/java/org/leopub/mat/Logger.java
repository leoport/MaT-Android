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
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class Logger {
    private static Logger sLogger = null;
    private FileWriter mWriter = null;
    @SuppressLint("SimpleDateFormat")
    private DateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Logger getInstance() {
        if (sLogger == null) {
            sLogger = new Logger(MyApplication.getAppContext());
        }
        return sLogger;
    }

    private Logger (Context context) {
        File file = new File(context.getExternalFilesDir(null) + Configure.LOG_FILENAME);
        try {
            mWriter = new FileWriter(file, true);
        } catch (IOException e) {
            Log.e("LOG", e.getMessage());
        }
    }

    private void write(Character level, String tag, String msg) {
        try {
            String date = mDateFormat.format(new Date());
            mWriter.write(level + " " + date + ": " + msg + "\n");
            mWriter.flush();
        } catch (Exception e) {
            Log.e("LOG", e.getMessage());
        }
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        getInstance().write('e', tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        getInstance().write('i', tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        //getInstance().write('d', tag, msg);
    }
}
