package com.skydown.android.data

import android.content.Context
import androidx.annotation.StringRes

object AppTextResolver {
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        if (::appContext.isInitialized) {
            return
        }
        appContext = context.applicationContext
    }

    fun string(@StringRes resId: Int, vararg formatArgs: Any): String {
        return if (formatArgs.isEmpty()) {
            appContext.getString(resId)
        } else {
            appContext.getString(resId, *formatArgs)
        }
    }
}
