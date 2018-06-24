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
package com.ucsunup.keepass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ucsunup.keepass.database.PwEntry;
import com.ucsunup.keepass.database.PwGroup;
import com.ucsunup.keepass.fragment.CardListAdapter;
import com.ucsunup.keepass.view.ClickView;
import com.ucsunup.keepass.view.PwEntryView;
import com.ucsunup.keepass.view.PwGroupView;

public class PwGroupListAdapter extends CardListAdapter<PwGroupListAdapter.ViewHolder> {

    private GroupBaseActivity mAct;
    private PwGroup mGroup;
    private List<PwGroup> groupsForViewing;
    private List<PwEntry> entriesForViewing;
    private Comparator<PwEntry> entryComp = new PwEntry.EntryNameComparator();
    private Comparator<PwGroup> groupComp = new PwGroup.GroupNameComparator();
    private SharedPreferences prefs;
    private OnPWMenuItemClickListener mOnPWMenuItemClickListener;

    private enum Type {
        GROUP, ENTRY
    }

    public interface OnPWMenuItemClickListener {
        boolean onMenuItemClick(MenuItem item, View v);
    }

    public PwGroupListAdapter(GroupBaseActivity act, PwGroup group,
                              OnPWMenuItemClickListener onPWMenuItemClickListener) {
        mAct = act;
        mGroup = group;
        mOnPWMenuItemClickListener = onPWMenuItemClickListener;
        prefs = PreferenceManager.getDefaultSharedPreferences(act);
        filterAndSort();
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                filterAndSort();
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (Type.values()[viewType]) {
            case GROUP:
                return new ViewHolder(PwGroupView.getInstance(mAct, null),
                        getListenerInfo().mOnClickListener, getListenerInfo().mOnLongClickListener,
                        mOnPWMenuItemClickListener);
            case ENTRY:
                return new ViewHolder(PwEntryView.getInstance(mAct, null, 0),
                        getListenerInfo().mOnClickListener, getListenerInfo().mOnLongClickListener,
                        mOnPWMenuItemClickListener);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int size = groupsForViewing.size();
        if (position < size) {
            // Group View
            PwGroup group = groupsForViewing.get(position);
            PwGroupView gv = (PwGroupView) holder.getItemView();
            gv.convertView(group);
            holder.setItemView(gv);
        } else {
            // Entry View
            PwEntry entry = entriesForViewing.get(position - size);
            PwEntryView ev = (PwEntryView) holder.getItemView();
            ev.convertView(entry, position);
            holder.setItemView(ev);
        }
    }

    @Override
    public int getItemCount() {
        return groupsForViewing.size() + entriesForViewing.size();
    }

    @Override
    public int getItemViewType(int position) {
        int size = groupsForViewing.size();
        if (position < size) {
            return Type.GROUP.ordinal();
        } else {
            return Type.ENTRY.ordinal();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        private OnClickListener mOnClickListener;
        private OnLongClickListener mOnLongClickListener;
        private OnPWMenuItemClickListener mOnPWMenuItemClickListener;
        private ClickView mItem;

        public ViewHolder(View itemView, OnClickListener onClickListener,
                          OnLongClickListener onLongClickListener,
                          OnPWMenuItemClickListener onPWMenuItemClickListener) {
            super(itemView);
            mOnClickListener = onClickListener;
            mOnLongClickListener = onLongClickListener;
            mOnPWMenuItemClickListener = onPWMenuItemClickListener;
            mItem = (ClickView) itemView;
            mItem.setOnClickListener(this);
            mItem.setOnLongClickListener(this);
            mItem.setOnCreateContextMenuListener(this);
        }

        public View getItemView() {
            return mItem;
        }

        public void setItemView(View itemView) {
            mItem = (ClickView) itemView;
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mOnLongClickListener != null) {
                mOnLongClickListener.onLongClick(v, getAdapterPosition());
                return true;
            }
            return false;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            if (v instanceof ClickView) {
                ((ClickView) v).onCreateMenu(menu, menuInfo);
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setOnMenuItemClickListener(this);
                }
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (mOnPWMenuItemClickListener != null
                    && mOnPWMenuItemClickListener.onMenuItemClick(item, mItem)) {
                return true;
            }
            return mItem.onContextItemSelected(item);
        }
    }

    private void filterAndSort() {
        entriesForViewing = new ArrayList<PwEntry>();

        for (int i = 0; mGroup.childEntries != null && i < mGroup.childEntries.size(); i++) {
            PwEntry entry = mGroup.childEntries.get(i);
            if (!entry.isMetaStream()) {
                entriesForViewing.add(entry);
            }
        }

        boolean sortLists = prefs.getBoolean(mAct.getString(R.string.sort_key), mAct.getResources().getBoolean(R.bool.sort_default));
        if (sortLists) {
            groupsForViewing = new ArrayList<PwGroup>(mGroup.childGroups);
            Collections.sort(entriesForViewing, entryComp);
            Collections.sort(groupsForViewing, groupComp);
        } else {
            groupsForViewing = mGroup.childGroups;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
