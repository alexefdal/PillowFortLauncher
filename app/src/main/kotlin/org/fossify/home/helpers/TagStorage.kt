package org.fossify.home.helpers

import android.content.Context

object TagStorage {
    private const val PREF_NAME = "app_tags"

    fun saveTags(context: Context, packageName: String, tags: List<String>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(packageName, tags.joinToString(",")).apply()
    }

    fun getTags(context: Context, packageName: String): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(packageName, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun searchAppsByTag(context: Context, query: String, allApps: List<org.fossify.home.models.AppLauncher>): List<org.fossify.home.models.AppLauncher> {
        return allApps.filter {
            getTags(context, it.packageName).any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }
}
