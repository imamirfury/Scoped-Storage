package com.amir.scopedstorage

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.amir.scopedstorage.adapter.InternalPhotoAdapter
import com.amir.scopedstorage.adapter.SharedPhotoAdapter
import com.amir.scopedstorage.databinding.ActivityHomeBinding
import com.amir.scopedstorage.extension.Permissions
import com.amir.scopedstorage.extension.isPermissionAvailable
import com.amir.scopedstorage.extension.sdk29OrAbove
import com.amir.scopedstorage.extension.toast
import com.amir.scopedstorage.model.InternalStoragePhoto
import com.amir.scopedstorage.model.SharedStoragePhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*


/***
 * Created By Amir Fury on January 17 2022
 *
 * Email: fury.amir93@gmail.com
 * */

class HomeActivity : AppCompatActivity() {

    private val shouldFetchInternalStoragePhotos = MutableLiveData(true)
    private val shouldFetchSharedStoragePhotos = MutableLiveData(true)
    private var isPrivate = false

    private val permission by lazy { arrayOf(Permissions.readStorage, Permissions.writeStorage) }


    private val binding by lazy {
        DataBindingUtil.inflate<ActivityHomeBinding>(
            LayoutInflater.from(this),
            R.layout.activity_home,
            null,
            false
        )
    }

    private val internalPhotoAdapter by lazy {
        InternalPhotoAdapter {
            val isDeleted = deletePhotoFromInternalStorage(it.name)
            if (isDeleted) {
                shouldFetchInternalStoragePhotos.postValue(true)
                toast("Photo Deleted Successfully")
            } else {
                toast("Failed To Delete Photo")
            }
        }
    }
    private val sharedPhotoAdapter by lazy {
        SharedPhotoAdapter {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.internalPhotosRecycler.adapter = internalPhotoAdapter
        binding.sharedPhotosRecycler.adapter = sharedPhotoAdapter

        lifecycleScope.launch {
            internalPhotoAdapter.submitList(loadPhotosFromInternalStorage())
            shouldFetchInternalStoragePhotos.postValue(false)
        }

//        shouldFetchInternalStoragePhotos.observe(this, {
//            if (it) {
//
//            }
//        })

        if (isPermissionAvailable(permission)) {
            CoroutineScope(Dispatchers.Main).launch {
                sharedPhotoAdapter.submitList(loadPhotosFromExternalStorage())
            }
        }

//        shouldFetchSharedStoragePhotos.observe(this, {
//            if (it) {
//                lifecycleScope.launch {
//                    if (isPermissionAvailable(permission)) {
//                        sharedPhotoAdapter.submitList(loadPhotosFromExternalStorage())
//                    } else {
//                        permissionsResultLauncher.launch(permission)
//                    }
//                }
//            }
//        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val itemSwitch = menu.findItem(R.id.switchActionBar)
        itemSwitch.setActionView(R.layout.layout_switch)
        val switchButton: SwitchCompat =
            menu.findItem(R.id.switchActionBar).actionView.findViewById(R.id.switchButton)
        switchButton.setOnCheckedChangeListener { _, b ->
            isPrivate = b
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.photo) {
            takePhotoResultContract.launch(null)
        }
        return super.onOptionsItemSelected(item)
    }

    private val permissionsResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            var isGranted = true
            permission.entries.forEach {
                if (!it.value) {
                    isGranted = it.value
                    return@forEach
                }
            }
            if (isGranted) {
                shouldFetchSharedStoragePhotos.postValue(true)
            }
        }

    private val takePhotoResultContract =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            val photoName = UUID.randomUUID().toString()
            val isSuccessful = when {
                isPrivate -> {
                    savePhotoToInternalStorage(photoName, bitmap)
                }
                isPermissionAvailable(permission) -> {
                    savePhotoToExternalStorage(photoName, bitmap)
                }
                else -> false
            }
            if (isSuccessful) {
                toast("Photo Saved Successfully")
                if (isPrivate) {
                    shouldFetchInternalStoragePhotos.postValue(true)
                } else {
                    shouldFetchInternalStoragePhotos.postValue(true)
                }
            } else {
                toast("Failed To Save Photo")
            }

        }

    private fun deletePhotoFromInternalStorage(photoName: String): Boolean {
        return try {
            deleteFile(photoName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun loadPhotosFromInternalStorage(): List<InternalStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val files = filesDir.listFiles()
            files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            } ?: listOf()
        }
    }


    private fun savePhotoToInternalStorage(photoName: String, bitmap: Bitmap): Boolean {
        return try {
            openFileOutput("$photoName.jpg", MODE_PRIVATE).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Could not save bitmap")
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun loadPhotosFromExternalStorage(): List<SharedStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val collection = sdk29OrAbove {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            val photos = mutableListOf<SharedStoragePhoto>()

            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)

                    val contentUri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    photos.add(SharedStoragePhoto(id, name, width, height, contentUri))
                }
                photos.toList()
            } ?: listOf()
        }
    }

    private fun savePhotoToExternalStorage(photoName: String, bitmap: Bitmap): Boolean {
        val photosCollection = sdk29OrAbove {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$photoName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        return try {
            contentResolver.insert(photosCollection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                        throw IOException("Could Not Save Bitmap")
                    }
                }
            } ?: throw IOException("Could Not Create Media Store Entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

}