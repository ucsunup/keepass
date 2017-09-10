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
package com.keepassdroid.fileselect;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.PasswordActivity;
import com.keepassdroid.compat.ContentResolverCompat;
import com.keepassdroid.compat.StorageAF;
import com.keepassdroid.fragment.AdvancedFileSelectFragment;
import com.keepassdroid.fragment.FillUsrPwdFragment;
import com.keepassdroid.settings.AppSettingsActivity;
import com.keepassdroid.utils.UriUtil;
import com.keepassdroid.utils.Util;

import java.io.File;
import java.net.URLDecoder;

public class FileSelectActivity extends AppCompatActivity implements View.OnClickListener, FillUsrPwdFragment.OnFragmentInteractionListener,
        AdvancedFileSelectFragment.OnFragmentInteractionListener {

    public static final int REQUEST_CODE_FILE_BROWSE = 1;
    public static final int REQUEST_CODE_GET_CONTENT = 2;
    public static final int OPEN_DOC = 3;

    // fragment tag
    public static final String TAG_FRAGMENT_FILLUSRPWDFRAGMENT = "fillusrpwdfragment";
    public static final String TAG_FRAGMENT_ADVANCEDFILESELECTFRAGMENT = "advancedfileselectfragment";

    private FragmentManager mFragmentManager;
    private FillUsrPwdFragment mFillUsrPwdFragment;
    private AdvancedFileSelectFragment mAdvancedFileSelectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.file_selection);

        // Load default database
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, PasswordActivity.DEFAULT_FILENAME);

        // Judge if not init databse file
        boolean needInitDBFile = true;
        File dbFile = new File(fileName);
        if (dbFile.exists()) {
            needInitDBFile = false;
        }

        mFragmentManager = this.getSupportFragmentManager();
        mFillUsrPwdFragment = FillUsrPwdFragment.newInstance(fileName, null, !needInitDBFile);
        mFragmentManager.beginTransaction().replace(R.id.content, mFillUsrPwdFragment)
                .addToBackStack(TAG_FRAGMENT_FILLUSRPWDFRAGMENT).commitAllowingStateLoss();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String filename = null;
        if (requestCode == REQUEST_CODE_FILE_BROWSE && resultCode == RESULT_OK) {
            filename = data.getDataString();
            if (filename != null) {
                if (filename.startsWith("file://")) {
                    filename = filename.substring(7);
                }

                filename = URLDecoder.decode(filename);
            }

        } else if ((requestCode == REQUEST_CODE_GET_CONTENT || requestCode == OPEN_DOC) && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    if (StorageAF.useStorageFramework(this)) {
                        try {
                            // try to persist read and write permissions
                            ContentResolver resolver = getContentResolver();
                            ContentResolverCompat.takePersistableUriPermission(resolver, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            ContentResolverCompat.takePersistableUriPermission(resolver, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (Exception e) {
                            // nop
                        }
                    }
                    if (requestCode == REQUEST_CODE_GET_CONTENT) {
                        uri = UriUtil.translate(this, uri);
                    }
                    filename = uri.toString();
                }
            }
        }

        if (filename != null) {
            EditText fn = (EditText) findViewById(R.id.file_filename);
            fn.setText(filename);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fileselect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_donate:
                try {
                    Util.gotoUrl(this, R.string.donate_url);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            case R.id.menu_db_advanced_setting:
                // 数据库的高级设置
                if (mFragmentManager != null) {
                    if (mAdvancedFileSelectFragment == null) {
                        mAdvancedFileSelectFragment = AdvancedFileSelectFragment.newInstance(null, null);
                    }
                    mFragmentManager.beginTransaction().replace(R.id.content, mAdvancedFileSelectFragment)
                            .addToBackStack(TAG_FRAGMENT_ADVANCEDFILESELECTFRAGMENT).commitAllowingStateLoss();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public String getCurrentFileName() {
        return null;
    }

    @Override
    public void setPwdForNewDatabase(String fileName, String keyFile) {
        // TODO: goto FillUsrPwdFragment for setting
        if (mFragmentManager != null) {
            mFragmentManager = getSupportFragmentManager();
        }
        if (mAdvancedFileSelectFragment == null) {
            mFillUsrPwdFragment = FillUsrPwdFragment.newInstance(fileName, keyFile, false);
        } else {
            Bundle args = mFillUsrPwdFragment.getArguments();
            args.clear();
            args.putString(FillUsrPwdFragment.ARG_FILENAME, fileName);
            args.putString(FillUsrPwdFragment.ARG_KEYFILE, keyFile);
        }
        mFragmentManager.beginTransaction().replace(R.id.content, mFillUsrPwdFragment)
                .addToBackStack(TAG_FRAGMENT_FILLUSRPWDFRAGMENT).commitAllowingStateLoss();
    }

    @Override
    public void openDatabase(String fileName, String keyFile) {
        if (mFillUsrPwdFragment == null) {
            mFillUsrPwdFragment = FillUsrPwdFragment.newInstance(fileName, keyFile, true);
        } else {
            Bundle args = mFillUsrPwdFragment.getArguments();
            args.putString(FillUsrPwdFragment.ARG_FILENAME, fileName);
            args.putString(FillUsrPwdFragment.ARG_KEYFILE, keyFile);
        }
        mFragmentManager.beginTransaction().replace(R.id.content, mFillUsrPwdFragment)
                .addToBackStack(TAG_FRAGMENT_FILLUSRPWDFRAGMENT).commitAllowingStateLoss();
    }
}
