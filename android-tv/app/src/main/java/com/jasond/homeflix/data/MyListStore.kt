package com.jasond.homeflix.data

import android.content.Context

class MyListStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("ds_cinema_my_list", Context.MODE_PRIVATE)

    fun load(): Set<Long> = preferences.getStringSet(KEY_IDS, emptySet()).orEmpty()
        .mapNotNull(String::toLongOrNull).toSet()

    fun save(ids: Set<Long>) {
        preferences.edit().putStringSet(KEY_IDS, ids.map(Long::toString).toSet()).apply()
    }

    private companion object { const val KEY_IDS = "movie_ids" }
}
