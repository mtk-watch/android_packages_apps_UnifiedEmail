/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import com.android.mail.R;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider.FolderCapabilities;
import com.android.mail.providers.UIProvider.FolderType;
import com.google.common.collect.Lists;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An adapter for translating a {@link FolderList} to a set of selectable views to be used for
 * applying folders to one or more conversations.
 */
public class FolderSelectorAdapter extends BaseAdapter {

    public static class FolderRow implements Comparable<FolderRow> {
        private final Folder mFolder;
        private boolean mIsPresent;

        public FolderRow(Folder folder, boolean isPresent) {
            mFolder = folder;
            mIsPresent = isPresent;
        }

        public Folder getFolder() {
            return mFolder;
        }

        public boolean isPresent() {
            return mIsPresent;
        }

        public void setIsPresent(boolean isPresent) {
            mIsPresent = isPresent;
        }

        @Override
        public int compareTo(FolderRow another) {
            // TODO: this should sort the system folders in the appropriate order
            if (equals(another)) {
                return 0;
            } else if (mIsPresent != another.mIsPresent) {
                return mIsPresent ? -1 : 1;
            } else {
                return mFolder.name.compareToIgnoreCase(another.mFolder.name);
            }
        }

    }

    protected List<FolderRow> mFolderRows = Lists.newArrayList();
    private LayoutInflater mInflater;
    private int mLayout;
    private final String mHeader;


    public FolderSelectorAdapter(Context context, Cursor folders,
            Set<String> initiallySelected, boolean single, String header) {
        mInflater = LayoutInflater.from(context);
        mLayout = single? R.layout.single_folders_view : R.layout.multi_folders_view;
        mHeader = header;
        createFolderRows(folders, initiallySelected);
    }

    protected void createFolderRows(Cursor folders, Set<String> initiallySelected) {
        if (folders == null) {
            return;
        }
        folders.moveToFirst();
        do {
            final Folder folder = new Folder(folders);
            if (meetsRequirements(folder)) {
                final FolderRow row =
                        new FolderRow(folder, initiallySelected.contains(folder.uri.toString()));
                mFolderRows.add(row);
            }
        } while (folders.moveToNext());
        Collections.sort(mFolderRows);
    }

    /**
     * Return whether the supplied folder meets the requirements to be displayed
     * in the folder list.
     */
    protected boolean meetsRequirements(Folder folder) {
        // We only want to show the non-Trash folders that can accept moved messages
        return folder.supportsCapability(FolderCapabilities.CAN_ACCEPT_MOVED_MESSAGES) &&
                folder.type != FolderType.TRASH;
    }

    @Override
    public int getCount() {
        return mFolderRows.size() + (hasHeader() ? 1 : 0);
    }

    @Override
    public Object getItem(int position) {
        if (position == 0 && hasHeader()) {
            return mHeader;
        }
        return mFolderRows.get(correctPosition(position));
    }

    @Override
    public long getItemId(int position) {
        if (hasHeader() && position == 0) {
            return -1;
        }
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (hasHeader() && position == 0) {
            return SeparatedFolderListAdapter.TYPE_SECTION_HEADER;
        } else {
            return SeparatedFolderListAdapter.TYPE_ITEM;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // check if position inside this section
        if (position == 0 && hasHeader()) {
            TextView view = (TextView) convertView;
            // Inflate the header view.
            if (view == null) {
                view = (TextView) mInflater.inflate(R.layout.folder_header, parent, false);
            }
            view.setText(mHeader);
            return view;
        }
        View view = convertView;
        CompoundButton checkBox = null;
        View colorBlock;
        ImageView iconView;

        if (view == null) {
            view = mInflater.inflate(mLayout, parent, false);
        }
        checkBox = (CompoundButton) view.findViewById(R.id.checkbox);
        // Suppress the checkbox selection, and handle the toggling of the
        // folder on the parent list item's click handler.
        checkBox.setClickable(false);
        colorBlock = view.findViewById(R.id.color_block);
        iconView = (ImageView) view.findViewById(R.id.folder_box);

        FolderRow row = (FolderRow) getItem(position);
        Folder folder = row.getFolder();
        checkBox.setText(folder.name);
        checkBox.setChecked(row.isPresent());

        Folder.setFolderBlockColor(folder, colorBlock);
        Folder.setIcon(folder, iconView);
        return view;
    }

    public boolean hasHeader() {
        return mHeader != null;
    }

    /**
     * Since this adapter may contain 2 types of data, make sure that we offset
     * the position being asked for correctly.
     */
    public int correctPosition(int position) {
        return hasHeader() ? position-1 : position;
    }

    public String getHeader() {
        return mHeader;
    }

}
