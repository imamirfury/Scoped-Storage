package com.amir.scopedstorage.extension

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


object Permissions {
    const val readStorage = Manifest.permission.READ_EXTERNAL_STORAGE
    const val writeStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE
}

fun Context.isPermissionAvailable(permissions: Array<String>): Boolean {
    permissions.forEach {
        if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}