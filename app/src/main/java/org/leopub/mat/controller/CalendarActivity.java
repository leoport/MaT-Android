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

package org.leopub.mat.controller;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import org.leopub.mat.R;
import org.leopub.mat.model.DateTime;

public class CalendarActivity extends Activity {
    private CalendarView mCalendarView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mCalendarView = new CalendarView(this);
        setContentView(mCalendarView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private class CalendarView extends View {
        final private float mContentOriginX = 80.0f;
        final private float mContentOriginY = 100.0f;
        private float mContentWidth;
        private float mContentHeight;
        private Paint mPaint;
        private Paint mDayHeaderText;
        private Paint mDayHeaderBg;
        private Paint mEventText;
        private Paint mEventBg;
        final private float DAY_HEADER_FONT_SIZE = 30.0f;
        final private float DAY_HEADER_PADDING = 10.0f;
        final private float HOUR_HEADER_FONT_SIZE = 30.0f;
        final private float HOUR_HEADER_PADDING = 10.0f;
        final private String days[] = getResources().getStringArray(R.array.calendar_days);
        final private String hours[] = getResources().getStringArray(R.array.calendar_hours);
        public CalendarView(Context context) {
            super(context);
            mPaint = new Paint();
            mPaint.setColor(0xff808080);
            mDayHeaderText = new Paint();
            mDayHeaderText.setColor(0xffffffff);
            mDayHeaderText.setTextSize(DAY_HEADER_FONT_SIZE);
            mDayHeaderBg = new Paint();
            mDayHeaderBg.setColor(0xff80c0ff);
            mEventText = new Paint();
            mEventText.setTextSize(30);
            mEventText.setColor(0xffffffff);
            mEventBg = new Paint();
            mEventBg.setColor(0xffffc0c0);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            mContentWidth = getWidth();
            mContentHeight = getHeight();
            float itemWidth = (mContentWidth - mContentOriginX) / days.length;
            float itemHeight = (mContentHeight - mContentOriginY) / hours.length;

            mPaint.setTextSize(30);
            canvas.drawRect(mContentOriginX, 0, mContentWidth, mContentOriginY, mDayHeaderBg);
            canvas.drawRect(0, 0, mContentOriginX, mContentHeight, mDayHeaderBg);
            canvas.drawText("19周", 0, mContentOriginY / 2, mDayHeaderText);
            float headerStartY = DAY_HEADER_FONT_SIZE + DAY_HEADER_PADDING;
            for (int i = 0; i < days.length; i++) {
                if (i == 0) {
                    mPaint.setStrokeWidth(5);
                } else {
                    mPaint.setStrokeWidth(3);
                }
                float x = mContentOriginX + i * itemWidth;
                canvas.drawLine(x, 0, x, mContentHeight, mPaint);
                canvas.drawText(days[i], x + DAY_HEADER_PADDING, headerStartY, mDayHeaderText);
            }
            for (int i = 0; i < hours.length; i++) {
                if (i == 0) {
                    mPaint.setStrokeWidth(3);
                } else {
                    mPaint.setStrokeWidth(1);
                }
                float y = mContentOriginY + i * itemHeight;
                canvas.drawLine(0, y, mContentWidth, y, mPaint);
                canvas.drawText(hours[i], DAY_HEADER_PADDING, y + headerStartY / 3, mDayHeaderText);
            }
            addEvent(canvas, new DateTime("2015-07-12 09:00:00"), new DateTime("2015-07-12 11:00:00"), "睡个觉", "10-202");
        }

        public void addEvent(Canvas canvas, DateTime startTime, DateTime endTime, String title, String place) {
            float itemWidth = (mContentWidth - mContentOriginX) / days.length;
            int dayOfWeek = startTime.getDayOfWeek();
            float left = mContentOriginX + dayOfWeek * itemWidth;
            float top = getYFromTime(startTime);
            float bottom = getYFromTime(endTime);
            float right = mContentOriginX + (dayOfWeek + 1) * itemWidth;
            canvas.drawRect(left, top, right, bottom, mEventBg);
            canvas.drawText(title, left, top + 30, mEventText);
            canvas.drawText(place, left, top + 65, mEventText);
        }

        private float getYFromTime(DateTime time) {
            float itemHeight = (mContentHeight - mContentOriginY) / hours.length;
            return mContentOriginY + (time.getHour() + (time.getMinute() / 60.0f) - 8.0f) * itemHeight / 2;
        }
    }
}

