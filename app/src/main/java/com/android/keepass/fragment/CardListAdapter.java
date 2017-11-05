package com.android.keepass.fragment;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by ucsunup on 2017/9/20.
 */

public abstract class CardListAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {

    ListenerInfo mListenerInfo;

    @Override
    public V onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(V holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    protected class ListenerInfo {
        public OnClickListener mOnClickListener;
        public OnLongClickListener mOnLongClickListener;
    }

    /**
     * Register a callback to be invoked when this view is clicked. If this view is not
     * clickable, it becomes clickable.
     *
     * @param l The callback that will run
     */
    public void setOnClickListener(@Nullable OnClickListener l) {
        getListenerInfo().mOnClickListener = l;
    }

    /**
     * Register a callback to be invoked when this view is clicked and held. If this view is not
     * long clickable, it becomes long clickable.
     *
     * @param l The callback that will run
     */
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        getListenerInfo().mOnLongClickListener = l;
    }

    /**
     * Interface definition for a callback to be invoked when a view is clicked.
     */
    public interface OnClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        void onClick(View v, int position);
    }

    /**
     * Interface definition for a callback to be invoked when a view has been clicked and held.
     */
    public interface OnLongClickListener {
        /**
         * Called when a view has been clicked and held.
         *
         * @param v The view that was clicked and held.
         * @return true if the callback consumed the long click, false otherwise.
         */
        boolean onLongClick(View v, int position);
    }

    protected ListenerInfo getListenerInfo() {
        if (mListenerInfo == null) {
            mListenerInfo = new ListenerInfo();
        }
        return mListenerInfo;
    }
}
