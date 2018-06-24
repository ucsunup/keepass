package com.ucsunup.keepass.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ucsunup.keepass.EntryEditActivity;
import com.ucsunup.keepass.ProgressTask;
import com.ucsunup.keepass.R;
import com.ucsunup.keepass.app.App;
import com.ucsunup.keepass.database.edit.FileOnFinish;
import com.ucsunup.keepass.database.edit.OnFinish;
import com.ucsunup.keepass.database.edit.SetPassword;
import com.ucsunup.keepass.utils.EmptyUtils;
import com.ucsunup.keepass.utils.UriUtil;

/**
 * Created by ucsunup on 2017/11/4.
 */

public class SetPasswordfDialog extends Activity {
    private byte[] masterKey;
    private Uri mKeyfile;
    private FileOnFinish mFinish;

    public static void Launch(Activity act) {
        Intent i = new Intent(act, SetPasswordfDialog.class);
        act.startActivityForResult(i, 0);
    }

    public byte[] getKey() {
        return masterKey;
    }

    public Uri keyfile() {
        return mKeyfile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_password);

        setTitle(R.string.password_title);

        // Ok button
        Button okButton = findViewById(R.id.ok);
        okButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TextView passView = findViewById(R.id.pass_password);
                String pass = passView.getText().toString();
                TextView passConfView = findViewById(R.id.pass_conf_password);
                String confpass = passConfView.getText().toString();

                // Verify that passwords match
                if (!pass.equals(confpass)) {
                    // Passwords do not match
                    Toast.makeText(SetPasswordfDialog.this, R.string.error_pass_match, Toast.LENGTH_LONG).show();
                    return;
                }

                TextView keyfileView = findViewById(R.id.pass_keyfile);
                Uri keyfile = UriUtil.parseDefaultFile(keyfileView.getText().toString());
                mKeyfile = keyfile;

                // Verify that a password or keyfile is set
                if (pass.length() == 0 && EmptyUtils.isNullOrEmpty(keyfile)) {
                    Toast.makeText(SetPasswordfDialog.this, R.string.error_nopass, Toast.LENGTH_LONG).show();
                    return;

                }

                SetPassword sp = new SetPassword(SetPasswordfDialog.this, App.getDB(), pass, keyfile, new AfterSave(mFinish, new Handler()));
                final ProgressTask pt = new ProgressTask(SetPasswordfDialog.this, sp, R.string.saving_database);
                boolean valid = sp.validatePassword(SetPasswordfDialog.this, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pt.run();
                    }
                });

                if (valid) {
                    pt.run();
                }
            }

        });

        // Cancel button
        Button cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mFinish != null) {
                    mFinish.run();
                }
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case EntryEditActivity.RESULT_OK_ICON_PICKER:
                break;
            case Activity.RESULT_CANCELED:
            default:
                break;
        }
    }

    private class AfterSave extends OnFinish {
        private FileOnFinish mFinish;

        public AfterSave(FileOnFinish finish, Handler handler) {
            super(finish, handler);
            mFinish = finish;
        }

        @Override
        public void run() {
            if (mSuccess) {
                if (mFinish != null) {
                    mFinish.setFilename(mKeyfile);
                }
                finish();
            } else {
                displayMessage(SetPasswordfDialog.this);
            }
            super.run();
        }
    }
}
