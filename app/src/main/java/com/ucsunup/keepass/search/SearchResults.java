/*
 * Copyright 2009 Brian Pellin.
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
package com.ucsunup.keepass.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.ucsunup.keepass.R;
import com.ucsunup.keepass.app.KeePass;
import com.ucsunup.keepass.Database;
import com.ucsunup.keepass.GroupBaseActivity;
import com.ucsunup.keepass.PwGroupListAdapter;
import com.ucsunup.keepass.app.App;
import com.ucsunup.keepass.database.PwGroup;
import com.ucsunup.keepass.view.GroupEmptyView;
import com.ucsunup.keepass.view.GroupViewOnlyView;

public class SearchResults extends GroupBaseActivity {

    private Database mDb;
    //private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setResult(KeePass.EXIT_NORMAL);

        mDb = App.getDB();
        // Likely the app has been killed exit the activity
        if (!mDb.Loaded()) {
            finish();
        }
        performSearch(getSearchStr(getIntent()));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        performSearch(getSearchStr(intent));
    }

    @Override
    protected void onClickGroup(PwGroup pwGroup) {
        //
    }

    private void performSearch(String query) {
        query(query.trim());
    }

    private void query(String query) {
        mGroup = mDb.Search(query);

        if (mGroup == null || mGroup.childEntries.size() < 1) {
            setContentView(new GroupEmptyView(this));
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

        setGroupTitle();
        setListAdapter(new PwGroupListAdapter(this, mGroup, null));
    }

	/*
    @Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		mQuery = getSearchStr(intent);
		performSearch();
		//mGroup = processSearchIntent(intent);
		//assert(mGroup != null);
	}
	*/

    private String getSearchStr(Intent queryIntent) {
        // get and process search query here
        final String queryAction = queryIntent.getAction();
        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            return queryIntent.getStringExtra(SearchManager.QUERY);
        }
        return "";
    }

    @Override
    protected void setGroupTitle() {
        findViewById(R.id.group_name).setVisibility(View.INVISIBLE);
        if (mGroup != null) {
            getSupportActionBar().setTitle(mGroup.getName());
        }
    }
}
