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
package com.ucsunup.keepass.view;


import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ucsunup.keepass.R;
import com.ucsunup.keepass.EntryActivity;
import com.ucsunup.keepass.GroupBaseActivity;
import com.ucsunup.keepass.ProgressTask;
import com.ucsunup.keepass.app.App;
import com.ucsunup.keepass.database.PwEntry;
import com.ucsunup.keepass.database.edit.DeleteEntry;
import com.ucsunup.keepass.settings.PrefsUtil;

public class PwEntryView extends ClickView {

    protected GroupBaseActivity mAct;
    protected PwEntry mPw;
    private TextView mTv;
    private int mPos;

    protected static final int MENU_OPEN = Menu.FIRST;
    private static final int MENU_DELETE = MENU_OPEN + 1;

    public static PwEntryView getInstance(GroupBaseActivity act, PwEntry pw, int pos) {
        return new PwEntryView(act, pw, pos);
    }

    protected PwEntryView(GroupBaseActivity act, PwEntry pw, int pos) {
        super(act);
        mAct = act;
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(lp);
        View ev = View.inflate(mAct, R.layout.entry_list_entry, this);
        mTv = (TextView) ev.findViewById(R.id.entry_text);
        mTv.setTextSize(PrefsUtil.getListTextSize(act));

        if (pw != null) {
            populateView(ev, pw, pos);
        }
    }

    private void populateView(View ev, PwEntry pw, int pos) {
        mPw = pw;
        mPos = pos;

        AvatarImageView iv = (AvatarImageView) ev.findViewById(R.id.entry_icon);
        App.getDB().drawFactory.assignDrawableTo(iv, mPw.getDisplayTitle(), getResources(), pw.getIcon());

        mTv.setText(mPw.getDisplayTitle());
    }

    public void convertView(PwEntry pw, int pos) {
        populateView(this, pw, pos);
    }

    public void refreshTitle() {
        mTv.setText(mPw.getDisplayTitle());
    }

    public void onClick() {
        launchEntry();
    }

    private void launchEntry() {
        EntryActivity.Launch(mAct, mPw, mPos);

    }

    private void deleteEntry() {
        Handler handler = new Handler();
        DeleteEntry task = new DeleteEntry(mAct, App.getDB(), mPw, mAct.new RefreshTask(handler));
        ProgressTask pt = new ProgressTask(mAct, task, R.string.saving_database);
        pt.run();

    }

    @Override
    public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
        menu.add(0, MENU_OPEN, 0, R.string.menu_open);
        if (!readOnly) {
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_OPEN:
                launchEntry();
                return true;
            case MENU_DELETE:
                deleteEntry();
                return true;
            default:
                return false;
        }
    }
}
