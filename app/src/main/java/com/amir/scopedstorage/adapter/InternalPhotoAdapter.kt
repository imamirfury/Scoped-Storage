package com.amir.scopedstorage.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.amir.scopedstorage.callbacks.DiffCallbacks
import com.amir.scopedstorage.databinding.ItemPhotoBinding
import com.amir.scopedstorage.holder.PhotosViewHolder
import com.amir.scopedstorage.model.InternalStoragePhoto


/***
 * Created By Amir Fury on January 17 2022
 *
 * Email: fury.amir93@gmail.com
 * */


class InternalPhotoAdapter(private val onPhotoClick: (InternalStoragePhoto) -> Unit) :
    ListAdapter<InternalStoragePhoto, PhotosViewHolder>(DiffCallbacks.internalPhotosDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotosViewHolder {
        return PhotosViewHolder(ItemPhotoBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: PhotosViewHolder, position: Int) {
        val photo = getItem(position)
        holder.binding.photo.apply {
            setImageBitmap(photo.bitmap)
            setOnLongClickListener {
                onPhotoClick(photo)
                true
            }
        }
    }
}