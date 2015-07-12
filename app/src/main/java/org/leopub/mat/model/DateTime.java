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

import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateTime implements Comparable<DateTime> {
    private static SimpleDateFormat sCompleteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static SimpleDateFormat sSimpleDateFormat   = new SimpleDateFormat("MM月dd日 HH点mm分", Locale.US);
    private static SimpleDateFormat sDigitDateFormat    = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);

    private GregorianCalendar mCalendar;

    public DateTime() {
        mCalendar = new GregorianCalendar();
        mCalendar.setTime(new Date());
    }

    public DateTime(long time) {
        mCalendar = new GregorianCalendar();
        mCalendar.setTime(new Date(time));
    }

    public DateTime(String s) {
        Date date;
        try {
            date = sCompleteDateFormat.parse(s);
        } catch (ParseException e) {
            date = new Date(0);
        }
        mCalendar = new GregorianCalendar();
        mCalendar.setTime(date);
    }

    @Override
    public int compareTo(@NonNull DateTime other) {
        return mCalendar.getTime().compareTo(other.mCalendar.getTime());
    }

    public String toCompleteString() {
        return sCompleteDateFormat.format(mCalendar.getTime());
    }

    public String toSimpleString() {
        return sSimpleDateFormat.format(mCalendar.getTime());
    }

    public String toDigitString() {
        return sDigitDateFormat.format(mCalendar.getTime());
    }

    public int getYear() {
        return mCalendar.get(Calendar.YEAR);
    }

    public void setYear(int year) {
        mCalendar.set(Calendar.YEAR, year);
    }

    public int getMonth() {
        return mCalendar.get(Calendar.MONTH);
    }

    public void setMonth(int month) {
        mCalendar.set(Calendar.MONTH, month);
    }

    public int getDay() {
        return mCalendar.get(Calendar.DAY_OF_MONTH);
    }

    public void setDay(int day) {
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
    }

    public int getHour() {
        return mCalendar.get(Calendar.HOUR_OF_DAY);
    }

    public void setHour(int hour) {
        mCalendar.set(Calendar.HOUR_OF_DAY, hour);
    }

    public int getMinute() {
        return mCalendar.get(Calendar.MINUTE);
    }

    public void setMinute(int minute) {
        mCalendar.set(Calendar.MINUTE, minute);
    }

    public int getDayOfWeek() {
        return mCalendar.get(Calendar.DAY_OF_WEEK);
    }
}
