package edu.vandy.app.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.AnyRes

/**
 * Generic mapping of any Android resource ID to a resource Uri.
 */
fun Context.getResourceUri(@AnyRes resId: Int): Uri {
    with(resources) {
        return with(Uri.Builder()) {
            scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            authority(getResourcePackageName(resId))
            appendPath(getResourceTypeName(resId))
            appendPath(getResourceEntryName(resId))
            build()
        }
    }
}

fun Context.toast(message: CharSequence): Toast = Toast
    .makeText(this, message, Toast.LENGTH_SHORT)
    .apply {
        show()
    }

fun Context.longToast(message: CharSequence): Toast = Toast
    .makeText(this, message, Toast.LENGTH_LONG)
    .apply {
        show()
    }

