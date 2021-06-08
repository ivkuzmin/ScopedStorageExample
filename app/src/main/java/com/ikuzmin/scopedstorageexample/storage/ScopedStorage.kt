package com.ikuzmin.scopedstorageexample.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import androidx.annotation.RequiresApi
import com.ikuzmin.scopedstorageexample.domain.MetaData

@RequiresApi(VERSION_CODES.Q)
class ScopedStorage(context: Context) : AndroidV28Storage(context) {

  override suspend fun moveToImages(uri: String, metaData: MetaData) {
    moveToMediaStorage(Uri.parse(uri), metaData, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
  }

  override suspend fun moveToDownloads(uri: String, metaData: MetaData) {
    moveToMediaStorage(Uri.parse(uri), metaData, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
  }

  @RequiresApi(VERSION_CODES.Q)
  override fun completeMoving(uri: Uri) {
    val updatePendingStateValue = ContentValues().apply {
      put(MediaColumns.IS_PENDING, 0)
    }
    contentResolver.update(uri, updatePendingStateValue, null, null)

  }

  override fun createContentValues(metaData: MetaData): ContentValues {
    return super.createContentValues(metaData).apply {
      put(MediaColumns.DATE_TAKEN, System.currentTimeMillis())
      put(MediaColumns.IS_PENDING, 1)
    }
  }

}