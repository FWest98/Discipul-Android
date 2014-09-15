package com.thomasdh.roosterpgplus.Data;

import android.app.SearchManager;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.support.annotation.NonNull;

public class SearchRecentsProvider extends SearchRecentSuggestionsProvider {
    public static final String AUTHORITY = "com.thomasdh.roosterpgplus.Data.SearchRecentsProvider";
    public static final int MODE = DATABASE_MODE_QUERIES;
    private String mIconUri = "android.resource://com.thomasdh.roosterpgplus/drawable/ic_action_time";

    public SearchRecentsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        class Wrapper extends CursorWrapper {
            Wrapper(Cursor c) {
                super(c);
            }

            @Override
            public String getString(int columnIndex) {
                if(columnIndex != -1 && columnIndex == getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1))
                    return mIconUri;
                return super.getString(columnIndex);
            }
        }

        return new Wrapper(super.query(uri, projection, selection, selectionArgs, sortOrder));
    }
}
