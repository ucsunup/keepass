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
package com.android.keepass.timeout;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.android.keepass.app.App;
import com.android.keepass.utils.Intents;

public class TimeoutService extends Service {
    private static final String TAG = "KeePassDroid Timer";
    private BroadcastReceiver mIntentReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(Intents.TIMEOUT)) {
                    timeout(context);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.TIMEOUT);
        registerReceiver(mIntentReceiver, filter);

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "Timeout service started");
    }

    private void timeout(Context context) {
        Log.d(TAG, "Timeout");
        App.setShutdown();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();

        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Timeout service stopped");

        unregisterReceiver(mIntentReceiver);
    }

    public class TimeoutBinder extends Binder {
        public TimeoutService getService() {
            return TimeoutService.this;
        }
    }

    private final IBinder mBinder = new TimeoutBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
