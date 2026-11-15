package com.fisalia.files.data.model

import android.content.Context
import androidx.annotation.StringRes
import com.fisalia.files.R

class UserMessageException(
    @StringRes val messageResId: Int,
    val formatArgs: List<Any> = emptyList(),
    cause: Throwable? = null
) : Exception(cause)

fun Throwable.toUserMessage(context: Context): String {
    var current: Throwable? = this
    while (current != null) {
        if (current is UserMessageException) {
            return context.getString(current.messageResId, *current.formatArgs.toTypedArray())
        }
        current = current.cause
    }
    return localizedMessage ?: context.getString(R.string.error_unknown)
}
