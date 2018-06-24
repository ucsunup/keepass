package com.ucsunup.keepass.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;

import com.ucsunup.keepass.R;

/**
 * Created by ucsunup on 2017/9/17.
 */

public class AnimatorUtils {
    public static void foldView(final View view, int height, final View switchView, final boolean show) {
        if (height == 0) {
            height = switchView.getHeight() - view.getHeight();
        }
        foldView(view, show ? height : 0, show ? 0 : height, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                switchView.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) {
                    ((ImageButton) switchView).setImageResource(R.drawable.arrow_down_light);
                } else {
                    ((ImageButton) switchView).setImageResource(R.drawable.arrow_up_light);
                }
                switchView.setEnabled(true);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public static void foldView(final View view, int from, int to, final Animator.AnimatorListener animationListener) {
        PropertyValuesHolder pvh = PropertyValuesHolder.ofFloat("translationY", from, to);
        ObjectAnimator showAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvh);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new LinearInterpolator());
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (view.getVisibility() != View.VISIBLE) {
                    view.setVisibility(View.VISIBLE);
                }
                if (animationListener != null) {
                    animationListener.onAnimationStart(animation);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (animationListener != null) {
                    animationListener.onAnimationEnd(animation);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (animationListener != null) {
                    animationListener.onAnimationCancel(animation);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                if (animationListener != null) {
                    animationListener.onAnimationRepeat(animation);
                }
            }
        });
        animatorSet.play(showAnimator);
        animatorSet.start();
    }
}
