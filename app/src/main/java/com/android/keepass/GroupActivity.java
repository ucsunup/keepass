/*
 * Copyright 2009-2014 Brian Pellin.
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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.keepass.app.KeePass;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.android.keepass.app.App;
import com.android.keepass.database.PwDatabase;
import com.android.keepass.database.PwDatabaseV3;
import com.android.keepass.database.PwDatabaseV4;
import com.android.keepass.database.PwGroup;
import com.android.keepass.database.PwGroupId;
import com.android.keepass.database.PwGroupV3;
import com.android.keepass.database.PwGroupV4;
import com.android.keepass.database.edit.AddGroup;
import com.android.keepass.dialog.ReadOnlyDialog;
import com.android.keepass.view.GroupViewOnlyView;
import com.android.keepass.view.PwGroupView;

public abstract class GroupActivity extends GroupBaseActivity {
    public static final String KEY_NAME = "name";
    public static final String KEY_ICON_ID = "icon_id";
    public static final int UNINIT = -1;

    protected boolean addGroupEnabled = false;
    protected boolean addEntryEnabled = false;
    protected boolean isRoot = false;
    protected boolean readOnly = false;

    private static final String TAG = "Group Activity:";

    public static void Launch(Activity act) {
        Launch(act, null);
    }

    public static void Launch(Activity act, PwGroup group) {
        Intent i;

        // Need to use PwDatabase since group may be null
        PwDatabase db = App.getDB().pm;
        if (db instanceof PwDatabaseV3) {
            i = new Intent(act, GroupActivityV3.class);

            if (group != null) {
                PwGroupV3 g = (PwGroupV3) group;
                i.putExtra(KEY_ENTRY, g.groupId);
            }
        } else if (db instanceof PwDatabaseV4) {
            i = new Intent(act, GroupActivityV4.class);

            if (group != null) {
                PwGroupV4 g = (PwGroupV4) group;
                i.putExtra(KEY_ENTRY, g.uuid.toString());
            }
        } else {
            // Reached if db is null
            Log.d(TAG, "Tried to launch with null db");
            return;
        }

        act.startActivityForResult(i, 0);
    }

    protected abstract PwGroupId retrieveGroupId(Intent i);

    protected void setupButtons() {
        addGroupEnabled = !readOnly;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setResult(KeePass.EXIT_NORMAL);

        Log.w(TAG, "Creating group view");
        Intent intent = getIntent();

        mGroupHistory.clear();
        PwGroupId id = retrieveGroupId(intent);
        refreshListView(id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        // For ListView ContextMenu
//        AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
//        ClickView cv = (ClickView) acmi.targetView;
//        cv.onCreateMenu(menu, menuInfo);
        super.onCreateContextMenu(menu, v, menuInfo);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // For ListView ContextMenu
//        AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
//        ClickView cv = (ClickView) acmi.targetView;
//
//        switch (item.getItemId()) {
//            case Menu.FIRST:
//                if (cv instanceof PwGroupView) {
//                    onClickGroup(((PwGroupView) cv).getPwGroup());
//                    return true;
//                }
//        }
//        return cv.onContextItemSelected(item);
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                String GroupName = data.getExtras().getString(GroupEditActivity.KEY_NAME);
                int GroupIconID = data.getExtras().getInt(GroupEditActivity.KEY_ICON_ID);
                GroupActivity act = GroupActivity.this;
                Handler handler = new Handler();
                AddGroup task = AddGroup.getInstance(this, App.getDB(), GroupName, GroupIconID, mGroup, act.new RefreshTask(handler), false);
                ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
                pt.run();
                break;

            case Activity.RESULT_CANCELED:
            default:
                break;
        }
    }

    protected void showWarnings() {
        if (App.getDB().readOnly) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            if (prefs.getBoolean(getString(R.string.show_read_only_warning), true)) {
                Dialog dialog = new ReadOnlyDialog(this);
                dialog.show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mGroupHistory.size() > 0) {
            refreshListView(getIdFromPwGroup(mGroupHistory.removeLast()));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onClickGroup(PwGroup pwGroup) {
        mGroupHistory.add(mGroup);
        mGroup = pwGroup;
        refreshListView(getIdFromPwGroup(mGroup));
    }

    private PwGroupId getIdFromPwGroup(PwGroup pwGroup) {
        PwDatabase db = App.getDB().pm;
        Intent i = new Intent();
        if (db instanceof PwDatabaseV3 && pwGroup != null) {
            PwGroupV3 g = (PwGroupV3) pwGroup;
            i.putExtra(KEY_ENTRY, g.groupId);
        } else if (db instanceof PwDatabaseV4 && pwGroup != null) {
            PwGroupV4 g = (PwGroupV4) pwGroup;
            i.putExtra(KEY_ENTRY, g.uuid.toString());
        } else {
            // Reached if db is null
            Log.d(TAG, "Tried to launch with null db");
            return null;
        }
        return retrieveGroupId(i);
    }

    private void refreshListView(PwGroupId id) {
        Database db = App.getDB();
        readOnly = db.readOnly;
        PwGroup root = db.pm.rootGroup;
        if (id == null) {
            mGroup = root;
        } else {
            mGroup = db.pm.groups.get(id);
        }

        Log.w(TAG, "Retrieved group");
        if (mGroup == null) {
            Log.w(TAG, "Group was null");
            if (db.pm instanceof PwDatabaseV4) {
                return;
            } else if (db.pm instanceof PwDatabaseV3) {
                mGroup = root;
            }
        }

        isRoot = mGroup == root;

        setupButtons();

        if (addGroupEnabled || addEntryEnabled) {
            setContentView(R.layout.group_add_entry);
        } else {
            setContentView(new GroupViewOnlyView(this));
        }
        // init ToolBar, and make AppBarLayout height == 2 * Toolbar.Height
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        appBarLayout.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams lp = appBarLayout.getLayoutParams();
                lp.height = 2 * getSupportActionBar().getHeight();
                appBarLayout.setLayoutParams(lp);
            }
        });

        Log.w(TAG, "Set view");
        final FloatingActionMenu editMenu = (FloatingActionMenu) findViewById(R.id.menu_group);
        FloatingActionButton addGroupBtn = (FloatingActionButton) findViewById(R.id.fab_submenu_add_group);
        editMenu.removeMenuButton(addGroupBtn);
        FloatingActionButton addEntryBtn = (FloatingActionButton) findViewById(R.id.fab_submenu_add_entry);
        editMenu.removeMenuButton(addEntryBtn);

        if (addGroupEnabled) {
            // Add Group button
            editMenu.addMenuButton(addGroupBtn);
            addGroupBtn.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    editMenu.close(true);
                    GroupEditActivity.Launch(GroupActivity.this);
                }
            });
        }

        if (addEntryEnabled) {
            // Add Entry button
            editMenu.addMenuButton(addEntryBtn);
            addEntryBtn.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    editMenu.close(true);
                    EntryEditActivity.Launch(GroupActivity.this, mGroup);
                }
            });
        }

        setGroupTitle();
        setGroupIcon();

        setListAdapter(new PwGroupListAdapter(this, mGroup, new PwGroupListAdapter.OnPWMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item, View v) {
                switch (item.getItemId()) {
                    case Menu.FIRST:
                        if (v instanceof PwGroupView) {
                            onClickGroup(((PwGroupView) v).getPwGroup());
                            return true;
                        }
                    default:
                        break;
                }
                return false;
            }
        }));
        registerForContextMenu(getListView());
        Log.w(TAG, "Finished creating group");

        if (isRoot) {
            showWarnings();
        }
    }
}
