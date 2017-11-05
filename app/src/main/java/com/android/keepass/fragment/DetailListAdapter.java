package com.android.keepass.fragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.keepass.R;
import com.android.keepass.view.PasswordTextViewSelect;
import com.android.keepass.view.TextViewSelect;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by ucsunup on 2017/9/20.
 */

public class DetailListAdapter extends CardListAdapter<DetailListAdapter.BaseViewHolder> {

    private final int TYPE_COMMON = 0;
    private final int TYPE_PASSWORD = 1;
    public static final int MIN_COUNT = 8;
    private ArrayList<Map.Entry<String, String>> mData;
    private Context mContext;
    private boolean mShowPassword = false;

    public DetailListAdapter(Context context, ArrayList data) {
        mContext = context;
        mData = data;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BaseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.detail_card_item, parent, false),
                getListenerInfo().mOnClickListener, getListenerInfo().mOnLongClickListener);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.getTitleView().setText(mData.get(position).getKey());
        if (position >= MIN_COUNT) {
            holder.getTitleView().setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.getTitleView().setBackgroundColor(mContext.getResources().getColor(R.color.fab_background_normal));
        }
        switch (getItemViewType(position)) {
            case TYPE_COMMON:
                holder.getContentView().setVisibility(View.VISIBLE);
                holder.getPasswordView().setVisibility(View.GONE);
                holder.getContentView().setText(mData.get(position).getValue());
                break;
            case TYPE_PASSWORD:
                holder.getContentView().setVisibility(View.GONE);
                holder.getPasswordView().setVisibility(View.VISIBLE);
                holder.getPasswordView().setText(mData.get(position).getValue());
                // set editview can not edit.
                holder.getPasswordView().setKeyListener(null);
                if (mShowPassword) {
                    holder.getPasswordView().setTransformationMethod(null);
                } else {
                    holder.getPasswordView().setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (mContext.getString(R.string.entry_password).equals(mData.get(position).getKey())) {
            return TYPE_PASSWORD;
        } else {
            return TYPE_COMMON;
        }
    }

    public void refreshData(ArrayList data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public ArrayList getData() {
        return mData;
    }

    public void showPassword(boolean showPassword) {
        mShowPassword = showPassword;
        notifyDataSetChanged();
    }

    public boolean isShowPassword() {
        return mShowPassword;
    }

    public void addMoreInfo(String key, String value) {
        mData.add(new AbstractMap.SimpleEntry(key, value));
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private OnClickListener mOnClickListener;
        private OnLongClickListener mOnLongClickListener;
        private TextView mTitle;
        private TextViewSelect mContent;
        private PasswordTextViewSelect mPassword;

        public BaseViewHolder(View itemView, OnClickListener onClickListener,
                              OnLongClickListener onLongClickListener) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.entry_title);
            mContent = itemView.findViewById(R.id.entry_content);
            mPassword = itemView.findViewById(R.id.entry_password);
            mOnClickListener = onClickListener;
            mOnLongClickListener = onLongClickListener;
            itemView.setOnClickListener(this);
        }

        public TextView getTitleView() {
            return mTitle;
        }

        public TextViewSelect getContentView() {
            return mContent;
        }

        public PasswordTextViewSelect getPasswordView() {
            return mPassword;
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v, getAdapterPosition());
            }
        }
    }
}
