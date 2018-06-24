package com.ucsunup.keepass.utils;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.design.widget.CheckableImageButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.ucsunup.keepass.R;

import java.lang.reflect.Field;

/**
 * Created by ucsunup on 2017/9/19.
 */

public class ViewUtils {
    /**
     * 当前如果不是密码输入模式，那么反射拿不到ToogleView，就会添加一个新的ToogleView
     *
     * @param context
     * @param view
     * @param resId
     * @param onClickListener
     * @return
     */
    public static View updateToogleView(final Context context, final TextInputLayout view, int resId, View.OnClickListener onClickListener) {
        if (context == null || view == null) {
            return null;
        }
        try {
            Field passwordToggleViewField = view.getClass().getDeclaredField("mPasswordToggleView");
            passwordToggleViewField.setAccessible(true);
            CheckableImageButton passwordToggleView = (CheckableImageButton) passwordToggleViewField.get(view);
            // InputType not password, so not show ImageDrable, make length of show all the same.
            if (passwordToggleView == null) {
                // InputFrame Reflect
                Field inputFrameField = view.getClass().getDeclaredField("mInputFrame");
                inputFrameField.setAccessible(true);
                FrameLayout inputFrame = (FrameLayout) inputFrameField.get(view);
                passwordToggleView = (CheckableImageButton) LayoutInflater.from(context)
                        .inflate(R.layout.design_text_input_password_icon, inputFrame, false);
                // 如果是自定义加入ToggleView这里需要反射设置click事件
                inputFrame.addView(passwordToggleView);

                final CheckableImageButton finalPasswordToggleView = passwordToggleView;
                passwordToggleView.post(new Runnable() {
                    @Override
                    public void run() {
                        EditText editText = view.getEditText();
                        // We need to add a dummy drawable as the end compound drawable so that the text is
                        // indented and doesn't display below the toggle view
                        Drawable dummyDrawable = new ColorDrawable();
                        dummyDrawable.setBounds(0, 0, Util.dip2px(context, finalPasswordToggleView.getMeasuredWidth()), 1);
                        Drawable[] compounds = TextViewCompat.getCompoundDrawablesRelative(editText);
                        TextViewCompat.setCompoundDrawablesRelative(editText, compounds[0], compounds[1],
                                dummyDrawable, compounds[3]);
                        // Copy over the EditText's padding so that we match
                        finalPasswordToggleView.setPadding(editText.getPaddingLeft(), editText.getPaddingTop(),
                                editText.getPaddingRight(), editText.getPaddingBottom());
                    }
                });
            }

            if (resId > 0) {
                passwordToggleView.setImageResource(resId);
            }
            if (onClickListener != null) {
                passwordToggleView.setOnClickListener(onClickListener);
            }
            return passwordToggleView;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
