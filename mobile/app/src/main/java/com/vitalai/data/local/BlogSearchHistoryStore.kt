package com.vitalai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.blogSearchHistoryStore: DataStore<Preferences> by preferencesDataStore(name = "blog_search_history")

@Singleton
class BlogSearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val history: Flow<List<String>> = context.blogSearchHistoryStore.data.map { preferences ->
        preferences[SEARCH_HISTORY]
            ?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    suspend fun save(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_QUERY_LENGTH) return

        context.blogSearchHistoryStore.edit { preferences ->
            val current = preferences[SEARCH_HISTORY]
                ?.split(SEPARATOR)
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            val next = listOf(normalizedQuery) +
                current.filterNot { it.equals(normalizedQuery, ignoreCase = true) }

            preferences[SEARCH_HISTORY] = next
                .take(MAX_HISTORY_ITEMS)
                .joinToString(SEPARATOR)
        }
    }

    suspend fun remove(query: String) {
        context.blogSearchHistoryStore.edit { preferences ->
            val next = preferences[SEARCH_HISTORY]
                ?.split(SEPARATOR)
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() && !it.equals(query.trim(), ignoreCase = true) }
                .orEmpty()

            if (next.isEmpty()) {
                preferences.remove(SEARCH_HISTORY)
            } else {
                preferences[SEARCH_HISTORY] = next.joinToString(SEPARATOR)
            }
        }
    }

    suspend fun clear() {
        context.blogSearchHistoryStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY)
        }
    }

    companion object {
        private const val MAX_HISTORY_ITEMS = 10
        private const val MIN_QUERY_LENGTH = 2
        private const val SEPARATOR = "\u001F"
        private val SEARCH_HISTORY = stringPreferencesKey("search_history")
    }
}
