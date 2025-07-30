package org.pillowfort.home.models

data class AppTag(
    val packageName: String,
    val tags: MutableList<String> = mutableListOf()
)
