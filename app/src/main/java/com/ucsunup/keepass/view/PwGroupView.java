/*
 * Copyright 2009-2012 Brian Pellin.
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


import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ucsunup.keepass.R;
import com.ucsunup.keepass.GroupActivity;
import com.ucsunup.keepass.GroupBaseActivity;
import com.ucsunup.keepass.app.App;
import com.ucsunup.keepass.database.PwGroup;
import com.ucsunup.keepass.database.PwGroupV3;
import com.ucsunup.keepass.settings.PrefsUtil;

public class PwGroupView extends ClickView {

    protected PwGroup mPw;
    protected GroupBaseActivity mAct;
    protected TextView mTv;

    protected static final int MENU_OPEN = Menu.FIRST;

    public static PwGroupView getInstance(GroupBaseActivity act, PwGroup pw) {
        if (pw instanceof PwGroupV3) {
            return new PwGroupViewV3(act, pw);
        } else {
            return new PwGroupView(act, pw);
        }
    }

    protected PwGroupView(GroupBaseActivity act, PwGroup pw) {
        super(act);
        mAct = act;
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(lp);
        View gv = View.inflate(act, R.layout.group_list_entry, this);
        mTv = (TextView) gv.findViewById(R.id.group_text);
        float size = PrefsUtil.getListTextSize(act);
        mTv.setTextSize(size);

        if (pw != null) {
            populateView(gv, pw);
        }
    }

    private void populateView(View gv, PwGroup pw) {
        mPw = pw;

        AvatarImageView iv = (AvatarImageView) gv.findViewById(R.id.group_icon);
        App.getDB().drawFactory.assignDrawableTo(iv, mPw.getName(), getResources(), pw.getIcon());

        mTv.setText(pw.getName());
    }

    public void convertView(PwGroup pw) {
        populateView(this, pw);
    }

    @Override
    public void onClick() {
        launchGroup();
    }

    public PwGroup getPwGroup() {
        return mPw;
    }

    private void launchGroup() {
        GroupActivity.Launch(mAct, mPw);
    }

    @Override
    public void onCreateMenu(ContextMenu menu, ContextMenuInfo menuInfo) {
        menu.add(0, MENU_OPEN, 0, R.string.menu_open);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_OPEN:
                launchGroup();
                return true;
            default:
                return false;
        }
    }

}