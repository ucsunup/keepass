package com.ucsunup.keepass.fragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.ucsunup.keepass.R;

/**
 * Created by ucsunup on 2017/9/20.
 */

public class TestViewHolder extends RecyclerView.ViewHolder {

    private CardListAdapter.OnClickListener mOnClickListener;
    private CardListAdapter.OnLongClickListener mOnLongClickListener;
    private TextView mContent;

    public TestViewHolder(View itemView, CardListAdapter.OnClickListener onClickListener,
                          CardListAdapter.OnLongClickListener onLongClickListener) {
        super(itemView);
        mContent = (TextView) itemView.findViewById(R.id.content);
        mOnClickListener = onClickListener;
        mOnLongClickListener = onLongClickListener;
    }

    public TextView getContentView() {
        return mContent;
    }
}
