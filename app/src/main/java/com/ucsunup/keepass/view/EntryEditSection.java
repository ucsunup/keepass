/*
 * Copyright 2013 Brian Pellin.
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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ucsunup.keepass.R;
import com.ucsunup.keepass.database.security.ProtectedString;

public class EntryEditSection extends RelativeLayout {

    public EntryEditSection(Context context) {
        super(context);
    }

    public EntryEditSection(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EntryEditSection(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

	/*
    public EntryEditSection(Context context, AttributeSet attrs, String title, ProtectedString value) {
		super(context, attrs);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflate(inflater, context, title, value);
		
		fillData(title, value);
	}
	*/

	/*
    private int getLayout() {
		return R.layout.entry_edit_section;
	}

	protected void inflate(LayoutInflater inflater, Context context,
			String title, ProtectedString value) {
		
		inflater.inflate(getLayout(), this);
	*/

    public void setData(String title, ProtectedString value) {
        setText(R.id.title, title);
        setText(R.id.value, value.toString());

        CheckBox cb = findViewById(R.id.protection);
        cb.setChecked(value.isProtected());
    }

    private void setText(int resId, String str) {
        if (str != null) {
            TextView tvTitle = findViewById(resId);
            tvTitle.setText(str);
        }
    }
}
