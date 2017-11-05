package com.android.keepass.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.keepass.R;

/**
 * Created by ucsunup on 2017/11/4.
 */

public class DialogActivity extends Activity {
    public static final String KEY_NAME = "name";
    public static final String KEY_ICON_ID = "icon_id";

    private int mSelectedIconID;
    private static DialogParams mDialogParam;

    public static final class DialogParams {
        public Context mContext;
        public String mTitle;
        public int mTitleId;
        public View mContentView;
        public int mContentViewId;
        public CharSequence mPositiveButtonText;
        public View.OnClickListener mPositiveButtonListener;
        public CharSequence mNegativeButtonText;
        public View.OnClickListener mNegativeButtonListener;
        public PreferenceManager.OnActivityResultListener mOnActivityResultListener;

        public DialogParams(Context context) {
            this.mContext = context;
        }
    }

    public static class Builder {
        private final DialogParams dp;

        public Builder(Context context) {
            this.dp = new DialogParams(context);
        }

        public Builder setTitle(@StringRes int resId) {
            this.dp.mTitleId = resId;
            return this;
        }

        public Builder setTitle(String title) {
            this.dp.mTitle = title;
            return this;
        }

        public Builder setContentView(@StringRes int resId) {
            this.dp.mContentViewId = resId;
            return this;
        }

        public Builder setContentView(View view) {
            this.dp.mContentView = view;
            return this;
        }

        public Builder setPositiveButton(@StringRes int textId, final View.OnClickListener listener) {
            this.dp.mTitle = this.dp.mContext.getString(textId);
            this.dp.mPositiveButtonListener = listener;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, final View.OnClickListener listener) {
            this.dp.mPositiveButtonText = text;
            this.dp.mPositiveButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(@StringRes int textId, final View.OnClickListener listener) {
            this.dp.mNegativeButtonText = this.dp.mContext.getText(textId);
            this.dp.mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, final View.OnClickListener listener) {
            this.dp.mNegativeButtonText = text;
            this.dp.mNegativeButtonListener = listener;
            return this;
        }

        public Builder setOnActivityResult(PreferenceManager.OnActivityResultListener onActivityResult) {
            this.dp.mOnActivityResultListener = onActivityResult;
            return this;
        }

        public void show() {
            mDialogParam = this.dp;
            Intent i = new Intent(this.dp.mContext, DialogActivity.class);
            ((Activity) this.dp.mContext).startActivityForResult(i, 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        if (mDialogParam == null) {
            return;
        }
        LinearLayout content = findViewById(R.id.content);
        if (mDialogParam.mContentViewId > 0) {
            content.addView(LayoutInflater.from(this).inflate(mDialogParam.mContentViewId, content, false));
        } else if (mDialogParam.mContentView != null) {
            content.addView(mDialogParam.mContentView);
        }

        if (mDialogParam.mTitleId > 0) {
            setTitle(mDialogParam.mTitleId);
        } else if (TextUtils.isEmpty(mDialogParam.mTitle)) {
            setTitle(mDialogParam.mTitle);
        }

        Button ok = findViewById(R.id.ok);
        ok.setText(TextUtils.isEmpty(mDialogParam.mPositiveButtonText) ? getString(android.R.string.ok) : mDialogParam.mPositiveButtonText);
        ok.setOnClickListener(mDialogParam.mPositiveButtonListener);

        Button cancel = findViewById(R.id.cancel);
        cancel.setText(TextUtils.isEmpty(mDialogParam.mNegativeButtonText) ? getString(android.R.string.cancel) : mDialogParam.mNegativeButtonText);
        cancel.setOnClickListener(mDialogParam.mNegativeButtonListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mDialogParam != null && mDialogParam.mOnActivityResultListener != null) {
            mDialogParam.mOnActivityResultListener.onActivityResult(requestCode, resultCode, data);
        }
    }
}
