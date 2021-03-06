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
package com.ucsunup.keepass.fileselect;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.ucsunup.keepass.AboutDialog;
import com.ucsunup.keepass.R;
import com.ucsunup.keepass.PasswordActivity;
import com.ucsunup.keepass.compat.ContentResolverCompat;
import com.ucsunup.keepass.compat.StorageAF;
import com.ucsunup.keepass.fragment.AdvancedDbEditFragment;
import com.ucsunup.keepass.fragment.AdvancedDbSelectFragment;
import com.ucsunup.keepass.fragment.LoginFragment;
import com.ucsunup.keepass.utils.Constants;
import com.ucsunup.keepass.utils.UriUtil;

import java.io.File;
import java.net.URLDecoder;

/**
 * @author ucsunup
 */
public class FileSelectActivity extends AppCompatActivity implements View.OnClickListener, LoginFragment.OnLoginListener,
        AdvancedDbSelectFragment.OnDbSelectListener, AdvancedDbEditFragment.OnDbEditListener {

    public static final int REQUEST_CODE_FILE_BROWSE = 1;
    public static final int REQUEST_CODE_GET_CONTENT = 2;
    public static final int OPEN_DOC = 3;

    // fragment tag
    public static final String TAG_FRAGMENT_LOGINFRAGMENT = "loginfragment";
    public static final String TAG_FRAGMENT_ADVANCEDFILESELECTFRAGMENT = "advancedfileselectfragment";

    private FragmentManager mFragmentManager;
    private boolean mLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.file_selection);

        // Load default database
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String fileName = prefs.getString(PasswordActivity.KEY_DEFAULT_FILENAME, Constants.DEFAULT_FILENAME);
        if (TextUtils.isEmpty(fileName)) {
            fileName = Constants.DEFAULT_FILENAME;
        }

        // Judge if not init databse file
        boolean needInitDBFile = true;
        File dbFile = new File(UriUtil.parseDefaultFile(fileName).getPath());
        if (dbFile.exists()) {
            needInitDBFile = false;
        }

        showLoginFragment(true, fileName, null, !needInitDBFile);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mLoginMode) {
            menu.findItem(R.id.menu_db_advanced_setting).setVisible(true);
        } else {
            menu.findItem(R.id.menu_db_advanced_setting).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
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
            case android.R.id.home:
                showLoginFragment(false, null, null, true);
                break;
            case R.id.menu_about:
                new AboutDialog(this).show();
                return true;
            case R.id.menu_db_advanced_setting:
                // 数据库的高级设置
                showAdvancedSettingFragment();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mLoginMode) {
            showLoginFragment(false, null, null, true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public String getCurrentFileName() {
        return null;
    }

    @Override
    public void createDatabase(String fileName, String keyFile) {
        // TODO: goto LoginFragment for setting
        showLoginFragment(true, fileName, keyFile, false);
    }

    @Override
    public void openDatabase(String fileName, String keyFile) {
        showLoginFragment(true, fileName, keyFile, true);
    }

    private void showLoginFragment(boolean refresh, String fileName, String keyFile, boolean login) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mLoginMode = true;
        getSupportActionBar().setTitle(R.string.app_name);
        if (mFragmentManager == null) {
            mFragmentManager = getSupportFragmentManager();
        }
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        Fragment settingFragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ADVANCEDFILESELECTFRAGMENT);
        if (settingFragment != null) {
            fragmentTransaction.hide(settingFragment);
        }
        Fragment loginFragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_LOGINFRAGMENT);
        Log.d("mmstest", "showLogin: " + fileName + ", " + keyFile);
        if (loginFragment == null || refresh) {
            fragmentTransaction.add(R.id.content,
                    LoginFragment.newInstance(fileName, keyFile, false),
                    TAG_FRAGMENT_LOGINFRAGMENT)
                    .commitAllowingStateLoss();
            return;
        } else if (refresh) {
//            Bundle args = loginFragment.getArguments();
//            args.clear();
//            args.putString(LoginFragment.ARG_FILENAME, fileName);
//            args.putString(LoginFragment.ARG_KEYFILE, keyFile);
//            args.putBoolean(LoginFragment.ARG_MODEL, login);
//            fragmentTransaction.replace(R.id.content,
//                    loginFragment,
//                    TAG_FRAGMENT_LOGINFRAGMENT)
//                    .commitAllowingStateLoss();
//            return;
        }
        fragmentTransaction.show(loginFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void showAdvancedSettingFragment() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mLoginMode = false;
        getSupportActionBar().setTitle(R.string.open_recent);
        if (mFragmentManager == null) {
            mFragmentManager = getSupportFragmentManager();
        }
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        Fragment loginFragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_LOGINFRAGMENT);
        if (loginFragment != null) {
            fragmentTransaction.hide(loginFragment);
        }

        Fragment settingFragment = mFragmentManager.findFragmentByTag(TAG_FRAGMENT_ADVANCEDFILESELECTFRAGMENT);
        if (settingFragment == null) {
            fragmentTransaction.add(R.id.content,
                    AdvancedDbSelectFragment.newInstance(null, null),
                    TAG_FRAGMENT_ADVANCEDFILESELECTFRAGMENT)
                    .commitAllowingStateLoss();
            return;
        }
        fragmentTransaction.show(settingFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }
}
