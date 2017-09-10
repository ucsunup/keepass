/*
 * Copyright 2009-2016 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV3;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.edit.AddEntry;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;
import com.keepassdroid.database.edit.UpdateEntry;
import com.keepassdroid.icons.Icons;
import com.keepassdroid.utils.Types;

public abstract class EntryEditActivity extends LockCloseHideActivity {
    public static final String KEY_ENTRY = "entry";
    public static final String KEY_PARENT = "parent";

    public static final int RESULT_OK_ICON_PICKER = 1000;
    public static final int RESULT_OK_PASSWORD_GENERATOR = RESULT_OK_ICON_PICKER + 1;

    protected PwEntry mEntry;
    private boolean mShowPassword = false;
    protected boolean mIsNew;
    protected int mSelectedIconID = -1;

    private EditText mTitle;
    private EditText mUrl;
    private EditText mUserName;
    private EditText mComment;
    private EditText mPassword;
    private EditText mConf;

    public static void Launch(Activity act, PwEntry pw) {
        Intent i;
        if (pw instanceof PwEntryV3) {
            i = new Intent(act, EntryEditActivityV3.class);
        } else if (pw instanceof PwEntryV4) {
            i = new Intent(act, EntryEditActivityV4.class);
        } else {
            throw new RuntimeException("Not yet implemented.");
        }

        i.putExtra(KEY_ENTRY, Types.UUIDtoBytes(pw.getUUID()));

        act.startActivityForResult(i, 0);
    }

    public static void Launch(Activity act, PwGroup pw) {
        Intent i;
        if (pw instanceof PwGroupV3) {
            i = new Intent(act, EntryEditActivityV3.class);
            EntryEditActivityV3.putParentId(i, KEY_PARENT, (PwGroupV3) pw);
        } else if (pw instanceof PwGroupV4) {
            i = new Intent(act, EntryEditActivityV4.class);
            EntryEditActivityV4.putParentId(i, KEY_PARENT, (PwGroupV4) pw);
        } else {
            throw new RuntimeException("Not yet implemented.");
        }

        act.startActivityForResult(i, 0);
    }

    protected abstract PwGroupId getParentGroupId(Intent i, String key);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mShowPassword = !prefs.getBoolean(getString(R.string.maskpass_key), getResources().getBoolean(R.bool.maskpass_default));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_edit);
        setResult(KeePass.EXIT_NORMAL);
        initView();

        // Likely the app has been killed exit the activity
        Database db = App.getDB();
        if (!db.Loaded()) {
            finish();
            return;
        }

        Intent i = getIntent();
        byte[] uuidBytes = i.getByteArrayExtra(KEY_ENTRY);

        PwDatabase pm = db.pm;
        if (uuidBytes == null) {

            PwGroupId parentId = getParentGroupId(i, KEY_PARENT);
            PwGroup parent = pm.groups.get(parentId);
            mEntry = PwEntry.getInstance(parent);
            mIsNew = true;

        } else {
            UUID uuid = Types.bytestoUUID(uuidBytes);
            assert (uuid != null);

            mEntry = pm.entries.get(uuid);
            mIsNew = false;

            fillData();
        }
    }

    private void initView() {
        View scrollView = findViewById(R.id.entry_scroll);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

        ImageButton iconButton = (ImageButton) findViewById(R.id.icon_button);
        iconButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                IconPickerActivity.Launch(EntryEditActivity.this);
            }
        });

        // Generate password button
        Button generatePassword = (Button) findViewById(R.id.generate_button);
        generatePassword.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                GeneratePasswordActivity.Launch(EntryEditActivity.this);
            }
        });

        // Save button
        Button save = (Button) findViewById(R.id.entry_save);
        save.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                EntryEditActivity act = EntryEditActivity.this;

                if (!validateBeforeSaving()) {
                    return;
                }

                PwEntry newEntry = populateNewEntry();

                if (newEntry.getTitle().equals(mEntry.getTitle())) {
                    setResult(KeePass.EXIT_REFRESH);
                } else {
                    setResult(KeePass.EXIT_REFRESH_TITLE);
                }

                RunnableOnFinish task;
                OnFinish onFinish = act.new AfterSave(new Handler());

                if (mIsNew) {
                    task = AddEntry.getInstance(EntryEditActivity.this, App.getDB(), newEntry, onFinish);
                } else {
                    task = new UpdateEntry(EntryEditActivity.this, App.getDB(), mEntry, newEntry, onFinish);
                }
                ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
                pt.run();
            }

        });

        // Cancel button
        Button cancel = (Button) findViewById(R.id.entry_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                finish();

            }

        });

        // Respect mask password setting
        if (mShowPassword) {
            EditText pass = ((TextInputLayout) findViewById(R.id.entry_password)).getEditText();
            EditText conf = ((TextInputLayout) findViewById(R.id.entry_confpassword)).getEditText();

            pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            conf.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }

        mTitle = ((TextInputLayout) findViewById(R.id.entry_title)).getEditText();
        mUrl = ((TextInputLayout) findViewById(R.id.entry_url)).getEditText();
        mUserName = ((TextInputLayout) findViewById(R.id.entry_user_name)).getEditText();
        mComment = ((TextInputLayout) findViewById(R.id.entry_comment)).getEditText();
        mPassword = ((TextInputLayout) findViewById(R.id.entry_password)).getEditText();
        mConf = ((TextInputLayout) findViewById(R.id.entry_confpassword)).getEditText();
    }

    protected boolean validateBeforeSaving() {
        // Require title
        String title = mTitle.getText().toString();
        if (title.length() == 0) {
            Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate password
        String pass = mPassword.getText().toString();
        String conf = mConf.getText().toString();
        if (!pass.equals(conf)) {
            Toast.makeText(this, R.string.error_pass_match, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    protected PwEntry populateNewEntry() {
        return populateNewEntry(null);
    }

    protected PwEntry populateNewEntry(PwEntry entry) {
        PwEntry newEntry;
        if (entry == null) {
            newEntry = mEntry.clone(true);
        } else {
            newEntry = entry;

        }

        Date now = Calendar.getInstance().getTime();
        newEntry.setLastAccessTime(now);
        newEntry.setLastModificationTime(now);

        PwDatabase db = App.getDB().pm;
        newEntry.setTitle(mTitle.getText().toString(), db);
        newEntry.setUrl(mUrl.getText().toString(), db);
        newEntry.setUsername(mUserName.getText().toString(), db);
        newEntry.setNotes(mComment.getText().toString(), db);
        newEntry.setPassword(mPassword.getText().toString(), db);

        return newEntry;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case RESULT_OK_ICON_PICKER:
                mSelectedIconID = data.getExtras().getInt(IconPickerActivity.KEY_ICON_ID);
                ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
                currIconButton.setImageResource(Icons.iconToResId(mSelectedIconID));
                break;

            case RESULT_OK_PASSWORD_GENERATOR:
                String generatedPassword = data.getStringExtra("com.keepassdroid.password.generated_password");
                mPassword.setText(generatedPassword);
                mConf.setText(generatedPassword);
                break;
            case Activity.RESULT_CANCELED:
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.entry_edit, menu);


        MenuItem togglePassword = menu.findItem(R.id.menu_toggle_pass);
        if (mShowPassword) {
            togglePassword.setTitle(R.string.menu_hide_password);
        } else {
            togglePassword.setTitle(R.string.menu_showpass);
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_pass:
                if (mShowPassword) {
                    item.setTitle(R.string.menu_showpass);
                    mShowPassword = false;
                } else {
                    item.setTitle(R.string.menu_hide_password);
                    mShowPassword = true;
                }
                setPasswordStyle();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setPasswordStyle() {

        if (mShowPassword) {
            mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            mConf.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        } else {
            mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            mConf.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    protected void fillData() {
        ImageButton currIconButton = (ImageButton) findViewById(R.id.icon_button);
        App.getDB().drawFactory.assignDrawableTo(currIconButton, getResources(), mEntry.getIcon());

        mTitle.setText(mEntry.getTitle());
        mUserName.setText(mEntry.getUsername());
        mUrl.setText(mEntry.getUrl());
        String password = new String(mEntry.getPassword());
        mPassword.setText(password);
        mConf.setText(password);
        setPasswordStyle();
        mComment.setText(mEntry.getNotes());
    }

    private final class AfterSave extends OnFinish {

        public AfterSave(Handler handler) {
            super(handler);
        }

        @Override
        public void run() {
            if (mSuccess) {
                finish();
            } else {
                displayMessage(EntryEditActivity.this);
            }
        }

    }

}
