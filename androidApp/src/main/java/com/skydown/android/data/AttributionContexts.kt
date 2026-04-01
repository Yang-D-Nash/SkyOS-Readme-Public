package com.skydown.android.data

import android.content.Context
import android.os.Build

fun Context.mediaAttributionContext(): Context {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        createAttributionContext("media")
    } else {
        this
    }
}
