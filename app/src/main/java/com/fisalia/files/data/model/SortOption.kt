package com.fisalia.files.data.model

enum class SortField {
    NAME,
    DATE,
    SIZE,
    TYPE
}

data class SortOption(
    val field: SortField = SortField.NAME,
    val isAscending: Boolean = true
)
