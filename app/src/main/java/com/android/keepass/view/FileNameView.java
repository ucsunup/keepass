/*
 * Copyright 2010-2011 Brian Pellin.
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
package com.android.keepass.view;

import android.animation.Animator;
import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.android.keepass.R;
import com.android.keepass.utils.AnimatorUtils;
import com.android.keepass.utils.ViewUtils;

public class FileNameView extends RelativeLayout {

    private View mToggleView;
    private OnClickListener mOnToggleViewClickListener;
    private boolean mShow = true;

    public FileNameView(Context context) {
        this(context, null);
    }

    public FileNameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context);
    }

    private void inflate(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.file_selection_filename, this);
        this.post(new Runnable() {
            @Override
            public void run() {
                mToggleView = ViewUtils.updateToogleView(getContext(),
                        (TextInputLayout) findViewById(R.id.file_filename_layout),
                        R.drawable.file, null);
                mToggleView.setOnClickListener(mOnToggleViewClickListener);
                final View allView = FileNameView.this;
                final ImageButton switchView = (ImageButton) findViewById(R.id.arrow_fold);
                final int translateHeight = allView.getHeight() - switchView.getHeight();
                switchView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mShow) {
                            AnimatorUtils.foldView(allView, translateHeight, 0, new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    switchView.setImageResource(R.drawable.arrow_down_light);
                                    mShow = false;
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                        } else {
                            AnimatorUtils.foldView(allView, 0, translateHeight, new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    switchView.setImageResource(R.drawable.arrow_up_light);
                                    mShow = true;
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            });
                        }
                    }
                });
                AnimatorUtils.foldView(allView, 0, translateHeight, null);
            }
        });
    }

    public void setOnToggleViewClickListener(OnClickListener onClickListener) {
        mOnToggleViewClickListener = onClickListener;
    }

//    public void updateExternalStorageWarning() {
//        int warning = -1;
//        String state = Environment.getExternalStorageState();
//        if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
//            warning = R.string.warning_read_only;
//        } else if (!state.equals(Environment.MEDIA_MOUNTED)) {
//            warning = R.string.warning_unmounted;
//        }
//
//        TextView tv = (TextView) findViewById(R.id.label_warning);
//        TextView label = (TextView) findViewById(R.id.label_open_by_filename);
//        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
//
//        if (warning != -1) {
//            tv.setText(warning);
//            tv.setVisibility(VISIBLE);
//
//            lp.addRule(RelativeLayout.BELOW, R.id.label_warning);
//        } else {
//            tv.setVisibility(INVISIBLE);
//        }
//
//        label.setLayoutParams(lp);
//    }
}
