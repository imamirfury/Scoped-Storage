package com.amir.scopedstorage.callbacks

import androidx.recyclerview.widget.DiffUtil
import com.amir.scopedstorage.model.InternalStoragePhoto
import com.amir.scopedstorage.model.SharedStoragePhoto



/***
 * Created By Amir Fury on January 17 2022
 *
 * Email: fury.amir93@gmail.com
 * */


object DiffCallbacks {


    val internalPhotosDiffCallback = object : DiffUtil.ItemCallback<InternalStoragePhoto>() {
        override fun areContentsTheSame(
            oldItem: InternalStoragePhoto,
            newItem: InternalStoragePhoto
        ): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areItemsTheSame(
            oldItem: InternalStoragePhoto,
            newItem: InternalStoragePhoto
        ): Boolean {
            return oldItem.name == newItem.name
        }
    }

    val sharedPhotoDiffCallback = object : DiffUtil.ItemCallback<SharedStoragePhoto>() {
        override fun areItemsTheSame(
            oldItem: SharedStoragePhoto,
            newItem: SharedStoragePhoto
        ): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(
            oldItem: SharedStoragePhoto,
            newItem: SharedStoragePhoto
        ): Boolean {
            return oldItem.name == newItem.name
        }

    }

}