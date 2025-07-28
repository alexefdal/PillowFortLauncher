package org.fossify.home.models

data class AppTag(
    val packageName: String,
    val tags: MutableList<String> = mutableListOf()
)
