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
package com.android.keepass;


import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.keepass.app.KeePass;
import com.android.keepass.app.App;
import com.android.keepass.compat.ActivityCompat;
import com.android.keepass.compat.EditorCompat;
import com.android.keepass.database.PwGroup;
import com.android.keepass.database.PwIconCustom;
import com.android.keepass.database.PwIconStandard;
import com.android.keepass.database.edit.OnFinish;
import com.android.keepass.dialog.SetPasswordfDialog;
import com.android.keepass.fragment.CardListAdapter;
import com.android.keepass.settings.AppSettingsActivity;
import com.android.keepass.view.ClickView;
import com.android.keepass.view.GroupViewOnlyView;
import com.android.keepass.view.PwGroupView;

import java.util.LinkedList;

public abstract class GroupBaseActivity extends LockCloseListActivity {
    protected RecyclerView mList;
    protected CardListAdapter mAdapter;

    public static final String KEY_ENTRY = "entry";
    public static final String KEY_MODE = "mode";

    private SharedPreferences prefs;

    protected LinkedList<PwGroup> mGroupHistory = new LinkedList<>();
    protected PwGroup mGroup;

    @Override
    protected void onResume() {
        super.onResume();
        refreshIfDirty();
    }

    public void refreshIfDirty() {
        Database db = App.getDB();
        if (db.dirty.contains(mGroup)) {
            db.dirty.remove(mGroup);
            mAdapter.notifyDataSetChanged();
        }
    }

    protected void onListItemClick(View v, int position) {
        ClickView cv = (ClickView) v;
        if (cv instanceof PwGroupView) {
            onClickGroup(((PwGroupView) cv).getPwGroup());
        } else {
            cv.onClick();
        }

    }

    protected abstract void onClickGroup(PwGroup pwGroup);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Likely the app has been killed exit the activity
        if (!App.getDB().Loaded()) {
            finish();
            return;
        }

        setContentView(new GroupViewOnlyView(this));
        setResult(KeePass.EXIT_NORMAL);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ActivityCompat.invalidateOptionsMenu(this);
    }

    protected void setGroupTitle() {
        if (mGroup != null) {
            StringBuilder nameBuilder = new StringBuilder("");
            for (PwGroup group : mGroupHistory) {
                nameBuilder.append(TextUtils.isEmpty(group.getName()) ? getString(R.string.app_name) : group.getName()).append("/");
            }
            nameBuilder.append(mGroup.getName());
            TextView tv = (TextView) findViewById(R.id.group_name);
            tv.setText(nameBuilder != null && nameBuilder.length() > 0 ? nameBuilder.toString() : getText(R.string.app_name));
            getSupportActionBar().setTitle("");
        }
    }

    protected void setGroupIcon() {
        if (mGroup != null && mGroup.getIcon() != null) {
            Drawable drawable;
            if (mGroup.getIcon() instanceof PwIconStandard) {
                drawable = App.getDB().drawFactory.getIconDrawable(getResources(), (PwIconStandard) mGroup.getIcon());
            } else {
                drawable = App.getDB().drawFactory.getIconDrawable(getResources(), (PwIconCustom) mGroup.getIcon());
            }
            getSupportActionBar().setIcon(drawable);
        }
    }

    protected void setListAdapter(CardListAdapter adapter) {
        ensureCorrectListView();
        mAdapter = adapter;
        mAdapter.setOnClickListener(new CardListAdapter.OnClickListener() {
            @Override
            public void onClick(View v, int position) {
                onListItemClick(v, position);
            }
        });
        mList.setAdapter(adapter);
    }

    protected RecyclerView getListView() {
        ensureCorrectListView();
        return mList;
    }

    private void ensureCorrectListView() {
        mList = (RecyclerView) findViewById(R.id.group_list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.group, menu);
        return true;
    }

    private void setSortMenuText(Menu menu) {
        boolean sortByName = false;

        // Will be null if onPrepareOptionsMenu is called before onCreate
        if (prefs != null) {
            sortByName = prefs.getBoolean(getString(R.string.sort_key), getResources().getBoolean(R.bool.sort_default));
        }

        int resId;
        if (sortByName) {
            resId = R.string.sort_db;
        } else {
            resId = R.string.sort_name;
        }

        MenuItem menuItem = menu.findItem(R.id.menu_sort);
        if (menuItem != null) {
            menuItem.setTitle(resId);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!super.onPrepareOptionsMenu(menu)) {
            return false;
        }
        setSortMenuText(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_lock:
                App.setShutdown();
                setResult(KeePass.EXIT_LOCK);
                finish();
                return true;
            case R.id.menu_search:
                onSearchRequested();
                return true;
            case R.id.menu_app_settings:
                AppSettingsActivity.Launch(this, true);
                return true;
            case R.id.menu_change_master_key:
                setPassword();
                return true;
            case R.id.menu_sort:
                toggleSort();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSort() {
        // Toggle setting
        String sortKey = getString(R.string.sort_key);
        boolean sortByName = prefs.getBoolean(sortKey, getResources().getBoolean(R.bool.sort_default));
        Editor editor = prefs.edit();
        editor.putBoolean(sortKey, !sortByName);
        EditorCompat.apply(editor);

        // Refresh menu titles
        ActivityCompat.invalidateOptionsMenu(this);

        // Mark all groups as dirty now to refresh them on load
        Database db = App.getDB();
        db.markAllGroupsAsDirty();
        // We'll manually refresh this group so we can remove it
        db.dirty.remove(mGroup);

        // Tell the adapter to refresh it's list
        mAdapter.notifyDataSetChanged();

    }

    private void setPassword() {
        SetPasswordfDialog.Launch(this);
    }

    public class RefreshTask extends OnFinish {
        public RefreshTask(Handler handler) {
            super(handler);
        }

        @Override
        public void run() {
            if (mSuccess) {
                refreshIfDirty();
            } else {
                displayMessage(GroupBaseActivity.this);
            }
        }
    }

    public class AfterDeleteGroup extends OnFinish {
        public AfterDeleteGroup(Handler handler) {
            super(handler);
        }

        @Override
        public void run() {
            if (mSuccess) {
                refreshIfDirty();
            } else {
                mHandler.post(new UIToastTask(GroupBaseActivity.this, "Unrecoverable error: " + mMessage));
                App.setShutdown();
                finish();
            }
        }
    }
}
