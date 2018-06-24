package com.ucsunup.keepass.fragment;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ucsunup.keepass.R;

import java.util.List;

/**
 * Created by ucsunup on 2017/9/20.
 */

public class BaseListAdapter extends CardListAdapter<BaseListAdapter.BaseViewHolder> {

    private List<String> mData;

    public BaseListAdapter(List<String> data) {
        mData = data;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BaseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.base_card_item, parent, false),
                getListenerInfo().mOnClickListener, getListenerInfo().mOnLongClickListener);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.getContentView().setText(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private CardListAdapter.OnClickListener mOnClickListener;
        private CardListAdapter.OnLongClickListener mOnLongClickListener;
        private TextView mContent;

        public BaseViewHolder(View itemView, CardListAdapter.OnClickListener onClickListener,
                              CardListAdapter.OnLongClickListener onLongClickListener) {
            super(itemView);
            mContent = itemView.findViewById(R.id.content);
            mOnClickListener = onClickListener;
            mOnLongClickListener = onLongClickListener;
            itemView.setOnClickListener(this);
        }

        public TextView getContentView() {
            return mContent;
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v, getAdapterPosition());
            }
        }
    }
}
