/*
 * Copyright 2009-2011 Brian Pellin.
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

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.android.keepass.utils.Util;

public class AboutDialog extends Dialog {

    public AboutDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle(R.string.app_name);

        setVersion();

        Button okButton = (Button) findViewById(R.id.donate_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    dismiss();
                    Util.gotoUrl(getContext(), R.string.donate_url);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), R.string.error_failed_to_launch_link, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        });
    }

    private void setVersion() {
        Context ctx = getContext();
        String version;
        try {
            PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            version = "";
        }
        TextView tv = (TextView) findViewById(R.id.version);
        tv.setText(version);
    }
}
