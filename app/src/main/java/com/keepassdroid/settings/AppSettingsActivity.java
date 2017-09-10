/*
 * Copyright 2009-2013 Brian Pellin.
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
package com.keepassdroid.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.AboutDialog;
import com.keepassdroid.Database;
import com.keepassdroid.LockingClosePreferenceActivity;
import com.keepassdroid.app.App;
import com.keepassdroid.compat.BackupManagerCompat;
import com.keepassdroid.database.PwEncryptionAlgorithm;
import com.keepassdroid.utils.Util;

public class AppSettingsActivity extends LockingClosePreferenceActivity {
    public static final String KEY_LOGIN_STATE = "login_state";
    public static boolean KEYFILE_DEFAULT = false;
    public boolean mLogin;

    private BackupManagerCompat backupManager;

    public static void Launch(Context ctx, boolean login) {
        Intent i = new Intent(ctx, AppSettingsActivity.class);
        i.putExtra(KEY_LOGIN_STATE, login);

        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init current login state
        mLogin = getIntent().getBooleanExtra(KEY_LOGIN_STATE, false);

        addPreferencesFromResource(R.xml.preferences);
        initPreference();
        backupManager = new BackupManagerCompat(this);

    }

    @Override
    protected void onStop() {
        backupManager.dataChanged();

        super.onStop();
    }

    private void initPreference() {
        if (mLogin) {
            Preference keyFile = findPreference(getString(R.string.keyfile_key));
            keyFile.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Boolean value = (Boolean) newValue;
                    if (!value.booleanValue()) {
                        App.getFileHistory().deleteAllKeys();
                    }
                    return true;
                }
            });

            Preference recentHistory = findPreference(getString(R.string.recentfile_key));
            recentHistory.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Boolean value = (Boolean) newValue;
                    if (value == null) {
                        value = true;
                    }
                    if (!value) {
                        App.getFileHistory().deleteAll();
                    }
                    return true;
                }
            });

            Database db = App.getDB();
            if (db.Loaded() && db.pm.appSettingsEnabled()) {
                Preference rounds = findPreference(getString(R.string.rounds_key));
                rounds.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        setRounds(App.getDB(), preference);
                        return true;
                    }
                });
                setRounds(db, rounds);
                Preference algorithm = findPreference(getString(R.string.algorithm_key));
                setAlgorithm(db, algorithm);
            } else {
                Preference dbSettings = findPreference(getString(R.string.db_key));
                dbSettings.setEnabled(false);
            }
        } else {
            PreferenceScreen databaseSetting = (PreferenceScreen) findPreference(getString(R.string.db_key));
            getPreferenceScreen().removePreference(databaseSetting);
            PreferenceScreen appSetting = (PreferenceScreen) findPreference(getString(R.string.app_key));
            getPreferenceScreen().removePreference(appSetting);
        }

        Preference donatePreference = findPreference(getString(R.string.menu_donate));
        donatePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Util.gotoUrl(AppSettingsActivity.this, R.string.donate_url);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(AppSettingsActivity.this, R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
                    return false;
                }
                return false;
            }
        });

        Preference aboutPreference = findPreference(getString(R.string.menu_about));
        aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AboutDialog dialog = new AboutDialog(AppSettingsActivity.this);
                dialog.show();
                return false;
            }
        });
    }

    private void setRounds(Database db, Preference rounds) {
        rounds.setSummary(Long.toString(db.pm.getNumRounds()));
    }

    private void setAlgorithm(Database db, Preference algorithm) {
        int resId;
        if (db.pm.getEncAlgorithm() == PwEncryptionAlgorithm.Rjindal) {
            resId = R.string.rijndael;
        } else {
            resId = R.string.twofish;
        }

        algorithm.setSummary(resId);
    }


}
