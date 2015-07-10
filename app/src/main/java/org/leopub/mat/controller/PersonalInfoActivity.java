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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.leopub.mat.Configure;
import org.leopub.mat.HttpUtil;
import org.leopub.mat.Logger;
import org.leopub.mat.R;
import org.leopub.mat.model.User;
import org.leopub.mat.model.UserManager;
import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.NetworkDataException;
import org.leopub.mat.exception.NetworkException;
import org.leopub.mat.model.Contact;
import org.leopub.mat.model.PersonalInfoItem;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PersonalInfoActivity extends ListActivity {
    private PersonalInfoArrayAdapter mPersonalInfoArrayAdapter;
    private List<PersonalInfoItem> mPersonalInfoList;
    private List<InfoCategoryItem> mInfoCategoryList;
    private List<Contact> mUnderlingList;
    private Contact mChosenPerson;
    private InfoCategoryItem mChosenCategory;

    private OnClickListener mEditButtonListener;
    private OnClickListener mHelpButtonListener;

    private UserManager mUserManager;
    private User mUser;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mUserManager = UserManager.getInstance();
        mUser = mUserManager.getCurrentUser();

        mEditButtonListener = new EditButtonListener();
        mHelpButtonListener = new HelpButtonListener();
        mPersonalInfoList = new ArrayList<>();
        mPersonalInfoArrayAdapter = null;

        mUnderlingList = mUser.getUnderling();
        mChosenPerson = mUser.getContact(mUserManager.getCurrentUser().getUserId());
        updateCategoryData();
        //updateView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.personal_info, menu);
        MenuItem menuItem = menu.findItem(R.id.person_name);
        if (mUnderlingList.size() == 0) {
            menuItem.setVisible(false);
        } else {
            menuItem.setTitle(mChosenPerson.getName());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.person_name) {
            onChoosePerson();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateCategoryData() {
        GetInfoCategoryTask getInfoCategoryTask= new GetInfoCategoryTask();
        getInfoCategoryTask.execute();
        //updateView();
    }

    public void updateCategoryView() {
        int n = mInfoCategoryList.size();
        String categories[] = new String[n];
        for (int i = 0; i < n; i++) {
            categories[i] = mInfoCategoryList.get(i).getName();
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
        spinner.setAdapter(categoryAdapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mChosenCategory = mInfoCategoryList.get(position);
                GetPersonalInfoTask getTask = new GetPersonalInfoTask();
                getTask.execute();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void onChoosePerson() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.my_underling));

        int n = mUnderlingList.size();
        String[] items = new String[n];
        for (int i = 0; i < n; i++) {
            Contact person = mUnderlingList.get(i);
            items[i] = person.getId() + " " + person.getName();
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mChosenPerson = mUnderlingList.get(which);
                invalidateOptionsMenu();
                updateCategoryData();
            }
        });
        builder.create().show(); 
    }

    public void updateView() {
        int pos = 0;
        if (mPersonalInfoArrayAdapter != null) {
            pos = getListView().getFirstVisiblePosition();
        }
        mPersonalInfoArrayAdapter = new PersonalInfoArrayAdapter(this, R.layout.personal_info_item, R.id.info_item_title, mPersonalInfoList);
        getListView().setAdapter(mPersonalInfoArrayAdapter);
        getListView().setSelection(pos);
    }

    private class PersonalInfoArrayAdapter extends ArrayAdapter<PersonalInfoItem> {
        public PersonalInfoArrayAdapter(Context context, int resource, int textViewId, List<PersonalInfoItem> items) {
            super(context, resource, textViewId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.personal_info_item, parent, false);
            }
            PersonalInfoItem item = getItem(position);
            TextView itemContentView = (TextView) convertView.findViewById(R.id.info_item_title) ;
            itemContentView.setText(item.getTitle());

            TextView itemInfoView = (TextView) convertView.findViewById(R.id.info_item_value);
            itemInfoView.setText(item.getValue());

            Button editButton = (Button) convertView.findViewById(R.id.info_item_edit);
            editButton.setTag(item.getKey());
            if (!item.isEditable()) {
                editButton.setVisibility(View.GONE);
            } else {
                editButton.setOnClickListener(mEditButtonListener);
                editButton.setVisibility(View.VISIBLE);
            }
            Button helpButton = (Button) convertView.findViewById(R.id.info_item_help);
            helpButton.setTag(item.getKey());
            if (item.getHint() == null || item.getHint().isEmpty()) {
                helpButton.setVisibility(View.GONE);
            } else {
                helpButton.setOnClickListener(mHelpButtonListener);
                helpButton.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    } 

    private List<PersonalInfoItem> parseJSON(String s) throws NetworkDataException {
        List<PersonalInfoItem> itemList = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(s);
            Iterator<String> iter = root.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                JSONObject obj = root.getJSONObject(key);
                obj.put("key", key);
                PersonalInfoItem item = new PersonalInfoItem(obj);
                itemList.add(item);
            }
        } catch (JSONException e) {
            throw new NetworkDataException("Parse the JSON of personal information failed");
        }
        Collections.sort(itemList);
        return itemList;
    }

    private class GetPersonalInfoTask extends AsyncTask<String, Void, String> {
        public GetPersonalInfoTask() {
            super();
        }

        @Override
        protected String doInBackground(String... args) {
            String response = "";
            try {
                User user = mUserManager.getCurrentUser();
                response = HttpUtil.getUrl(user, mChosenCategory.getUrl());
                mPersonalInfoList = parseJSON(response);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PersonalInfoActivity.this.updateView();
                    }
                 });
            } catch (NetworkException e) {
                showToastHint(getString(R.string.error_network));
            } catch (AuthException e) {
                showToastHint(getString(R.string.error_auth_fail));
            } catch (NetworkDataException e) {
                Logger.i("PERSON INFO", "GET PERSONAL INFO FAILED:" + response);
                showToastHint(getString(R.string.error_network_data));
            }
            return "";
        }
        private void showToastHint(final String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    Toast.makeText(PersonalInfoActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }
    }

    private class UpdatePersonalInfoTask extends AsyncTask<String, Void, String> {
        public UpdatePersonalInfoTask() {
            super();
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                String res = HttpUtil.postURL(mUserManager.getCurrentUser(), mChosenCategory.getUrl(), args[0]);
                mPersonalInfoList = parseJSON(res);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PersonalInfoActivity.this.updateView();
                    }
                 });
            } catch (NetworkException e) {
                showToastHint(getString(R.string.error_network));
            } catch (AuthException e) {
                showToastHint(getString(R.string.error_auth_fail));
            } catch (NetworkDataException e) {
                showToastHint(getString(R.string.error_network_data));
            }
            return "";
        }

        private void showToastHint(final String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    Toast.makeText(PersonalInfoActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }
    }

    private class EditButtonListener implements OnClickListener {
        private PersonalInfoItem mInfoItem;

        @Override
        public void onClick(View view) {
            String s = (String) view.getTag();
            for (PersonalInfoItem item : mPersonalInfoList) {
                if (item.getKey().equals(s)) {
                    mInfoItem = item;
                    if (mInfoItem.getType() == PersonalInfoItem.InfoType.select) {
                        buildOptionDialog();
                    } else if(mInfoItem.getType() == PersonalInfoItem.InfoType.date){
                        String value = item.getValue();
                        if (value == null || value.length() == 0) {
                            value = new Date().toString();
                        }
                        Scanner scanner = new Scanner(value);
                        scanner.useDelimiter("-");
                        int year = scanner.nextInt();
                        int month = scanner.nextInt();
                        int day = scanner.nextInt();
                        scanner.close();
                        buildDatePickerDialog(year, month, day);
                    } else {
                        buildTextInputDialog();
                    }
                    break;
                }
            }
        }

        private void update(String newValue) {
            try {
                String params = "key=" + mInfoItem.getKey() + "&value=" + URLEncoder.encode(newValue, "utf-8");
                UpdatePersonalInfoTask updateTask = new UpdatePersonalInfoTask();
                updateTask.execute(params);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        private void buildDatePickerDialog(int year, int month, int day) {
            new DatePickerDialog(PersonalInfoActivity.this, new OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker picker, int year, int month, int day) {
                    String newValue = year + "-" + (month + 1) + "-" + day;
                    update(newValue);
                }
                
            }, year, month - 1, day).show();
        }
        private void buildOptionDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(PersonalInfoActivity.this);
            builder.setTitle(mInfoItem.getTitle());
            builder.setItems(mInfoItem.getOptions().split(";"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newValue = mInfoItem.getOptions().split(";")[which].split(":")[0];
                    update(newValue);
                }
            });
            builder.create().show(); 
        }
        private void buildTextInputDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(PersonalInfoActivity.this);
            builder.setTitle(mInfoItem.getTitle());

            // Set up the input
            final EditText input = new EditText(PersonalInfoActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(mInfoItem.getValue());
            input.setHint(mInfoItem.getHint());
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newValue = input.getText().toString();
                    update(newValue);
                    //Toast.makeText(PersonalInfoActivity.this, newValue, Toast.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }

    private class HelpButtonListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            String s = (String) view.getTag();
            for (PersonalInfoItem item : mPersonalInfoList) {
                if (item.getKey().equals(s)) {
                    buildTextDialog(item);
                    break;
                }
            }
        }

        private void buildTextDialog(PersonalInfoItem item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PersonalInfoActivity.this);
            builder.setTitle(item.getTitle());

            // Set up the input
            final TextView textView = new TextView(PersonalInfoActivity.this);
            textView.setText(item.getHint());
            textView.setTextSize(20);
            builder.setView(textView);
            builder.show();
        }
    }
    private class GetInfoCategoryTask extends AsyncTask<String, Void, String> {
        public GetInfoCategoryTask() {
            super();
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                String res = mUser.getCategoryJSON(mChosenPerson.getId());
                mInfoCategoryList = parseJSON(res);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCategoryView();
                    }
                 });
            } catch (NetworkException e) {
                showToastHint(getString(R.string.error_network));
            } catch (AuthException e) {
                showToastHint(getString(R.string.error_auth_fail));
            } catch (NetworkDataException e) {
                showToastHint(getString(R.string.error_network_data));
            }
            return "";
        }
        private void showToastHint(final String message) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                    Toast.makeText(PersonalInfoActivity.this, message, Toast.LENGTH_LONG).show();
               }
            });
        }

        private List<InfoCategoryItem> parseJSON(String s) throws NetworkDataException {
            List<InfoCategoryItem> itemList = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(s);
                int n = array.length();
                for (int i = 0; i < n; i++) {
                    JSONObject obj = array.getJSONObject(i);
                    InfoCategoryItem item = new InfoCategoryItem(obj);
                    if (item.getType().equals("person")) {
                        itemList.add(item);
                    }
                }
            } catch (JSONException e) {
                Logger.i("CATEGORY", e.getMessage());
                throw new NetworkDataException("Parse the JSON of categories failed");
            }
            return itemList;
        }
    }

    private class InfoCategoryItem {
        private String mKey;
        private String mName;
        private String mType;

        public InfoCategoryItem(JSONObject obj) throws JSONException {
            mKey = obj.getString("key");
            mName = obj.getString("name");
            mType = obj.getString("type");
        }
        public String getName() {
            return mName;
        }
        public String getType() {
            return mType;
        }
        public String getUrl() {
            return Configure.INFO_PERSON_URL + mKey + ".php?" + "id=" + mChosenPerson.getId();
        }
    }
}
