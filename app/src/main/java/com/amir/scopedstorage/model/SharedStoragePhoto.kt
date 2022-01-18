package com.amir.scopedstorage.model

import android.net.Uri



/***
 * Created By Amir Fury on January 17 2022
 *
 * Email: fury.amir93@gmail.com
 * */

data class SharedStoragePhoto(
    val id: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val contentUri: Uri
)