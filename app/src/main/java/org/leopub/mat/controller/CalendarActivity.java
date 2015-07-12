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

public class CalendarActivity extends Activity {
    final private float mContentOriginX = 100.0f;
    final private float mContentOriginY = 100.0f;
    private float mContentWidth;
    private float mContentHeight;
    final private int N_DAY_PER_WEEK = 7;
    final private int N_ROW = 8;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(new CalendarView(this));
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
        private Paint mPaint;
        private Paint mDayHeaderText;
        private Paint mDayHeaderBg;
        final private float DAY_HEADER_FONT_SIZE = 30.0f;
        final private float DAY_HEADER_PADDING = 10.0f;
        public CalendarView(Context context) {
            super(context);
            mPaint = new Paint();
            mPaint.setColor(0xff808080);
            mDayHeaderText = new Paint();
            mDayHeaderText.setColor(0xffffffff);
            mDayHeaderText.setTextSize(DAY_HEADER_FONT_SIZE);
            mDayHeaderBg = new Paint();
            mDayHeaderBg.setColor(0xff80c0ff);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            mContentWidth = getWidth();
            mContentHeight = getHeight();
            float itemWidth = (mContentWidth - mContentOriginX) / N_DAY_PER_WEEK;
            float itemHeight = (mContentHeight - mContentOriginY) / N_ROW;

            mPaint.setTextSize(30);
            canvas.drawRect(mContentOriginX, 0, mContentWidth, mContentOriginY, mDayHeaderBg);
            canvas.drawText("19周", 0, 100, mDayHeaderText);
            float headerStartY = DAY_HEADER_FONT_SIZE + DAY_HEADER_PADDING;
            for (int i = 0; i < N_DAY_PER_WEEK; i++) {
                if (i == 0) {
                    mPaint.setStrokeWidth(5);
                } else {
                    mPaint.setStrokeWidth(3);
                }
                float x = mContentOriginX + i * itemWidth;
                canvas.drawLine(x, 0, x, mContentHeight, mPaint);
                canvas.drawText(String.valueOf(i + 1), x + DAY_HEADER_PADDING, headerStartY, mDayHeaderText);
            }
            for (int i = 0; i < N_ROW; i++) {
                if (i == 0) {
                    mPaint.setStrokeWidth(3);
                } else {
                    mPaint.setStrokeWidth(1);
                }
                float y = mContentOriginY + i * itemHeight;
                canvas.drawLine(0, y, mContentWidth, y, mPaint);
            }
        }
    }
}

