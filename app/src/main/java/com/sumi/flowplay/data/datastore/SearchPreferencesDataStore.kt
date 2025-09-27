package com.sumi.flowplay.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "search_prefs")
    private val SEARCH_KEY = stringSetPreferencesKey("recent_searches")

    val recentSearches: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            prefs[SEARCH_KEY]?.toList()?.reversed() ?: emptyList()
        }

    suspend fun addSearch(keyword: String, maxSize: Int = 10) {
        context.dataStore.edit { prefs ->
            val current = prefs[SEARCH_KEY]?.toMutableSet() ?: mutableSetOf()
            current.remove(keyword)  // 중복 제거
            current.add(keyword)     // 맨 위로 추가
            if (current.size > maxSize) {
                // 오래된 검색어 제거
                val toRemove = current.size - maxSize
                current.drop(toRemove).toSet()
            }
            prefs[SEARCH_KEY] = current
        }
    }
}