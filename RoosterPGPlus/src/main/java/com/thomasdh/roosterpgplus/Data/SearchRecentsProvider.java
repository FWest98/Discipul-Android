package com.thomasdh.roosterpgplus.Data;

import android.content.SearchRecentSuggestionsProvider;

public class SearchRecentsProvider extends SearchRecentSuggestionsProvider {
    public static final String AUTHORITY = "com.thomasdh.roosterpgplus.Data";
    public static final int MODE = DATABASE_MODE_QUERIES;

    public SearchRecentsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
