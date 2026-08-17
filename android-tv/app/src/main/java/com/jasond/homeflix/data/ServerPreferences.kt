package com.jasond.homeflix.data

import android.content.Context

class ServerPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("homeflix", Context.MODE_PRIVATE)

    fun loadServerUrl(): String? = preferences.getString(SERVER_URL, null)

    fun saveServerUrl(url: String) {
        preferences.edit().putString(SERVER_URL, url).apply()
    }

    companion object {
        private const val SERVER_URL = "server_url"
    }
}
