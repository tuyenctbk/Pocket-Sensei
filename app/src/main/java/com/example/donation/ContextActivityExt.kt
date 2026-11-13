package com.example.donation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Unwraps ContextWrappers (e.g. Dialog/Theme wrappers in Jetpack Compose) to retrieve
 * the underlying Activity required by Google Play Billing's launchBillingFlow().
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
