package com.ikuzmin.scopedstorageexample.storage

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.provider.OpenableColumns
import androidx.annotation.RequiresPermission
import androidx.core.content.FileProvider
import com.ikuzmin.scopedstorageexample.domain.MetaData
import com.ikuzmin.scopedstorageexample.remote.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

open class AndroidV28Storage(private val context: Context) : Storage {

  protected val contentResolver = context.contentResolver

  override suspend fun metaData(uri: String): MetaData {
    var size = 0
    var filename = ""
    var mimeType = ""
    contentResolver.query(Uri.parse(uri), null, null, null, null)?.use {
      val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
      it.moveToFirst()
      size = it.getLong(sizeIndex).toInt()
      filename = it.getString(nameIndex)
      mimeType = contentResolver.getType(Uri.parse(uri))!!
    }
    return MetaData(size, filename, mimeType)
  }

  override suspend fun saveFile(response: Response): Flow<SaveProgress> {
    return flow {
      val cacheFile = CacheFile(context, response.filename).create()
      val fileOutputStream = FileOutputStream(cacheFile)
      val size = response.size
      response.content.use {
        var byteArray = ByteArray(255)
        var currentSize = 0
        while (true) {
          val byteCount = it.read(byteArray)
          if (byteCount < 0) break
          fileOutputStream.write(byteArray)
          currentSize += byteCount
          emit(Intermediate(((currentSize.toFloat() / size) * 100).toInt()))
        }
      }
      fileOutputStream.close()
      emit(
        Complete(
          FileProvider.getUriForFile(
            context,
            "com.ikuzmin.scopedstorageexample.fileprovider",
            cacheFile
          ).toString()
        )
      )
    }
  }

  override suspend fun moveToImages(uri: String, metaData: MetaData) {
    moveToMediaStorage(Uri.parse(uri),metaData,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
  }

  override suspend fun moveToDownloads(uri: String, metaData: MetaData) {
    val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val fileInDownload =  File(downloadDirectory, metaData.filename)
    val originFd = contentResolver.openFileDescriptor(Uri.parse(uri), "r")
    moveToFile(originFd!!,fileInDownload)
  }

  protected open fun moveToMediaStorage(uri: Uri, metaData: MetaData, mediaStoreUri: Uri) {
    val values = createContentValues(metaData)
    val fileInDownloadUri =
      contentResolver.insert(mediaStoreUri, values)
    moveToFile(uri, fileInDownloadUri!!)
    completeMoving(fileInDownloadUri)
  }

  protected open fun completeMoving(uri:Uri){
    //do nothing
  }

  private fun moveToFile(originUri: Uri, destinationUri: Uri) {
    val originFd = contentResolver.openFileDescriptor(originUri, "r")
    val destinationFd = contentResolver.openFileDescriptor(destinationUri, "w")
    val source = FileInputStream(originFd!!.fileDescriptor)
    val dstination = FileOutputStream(destinationFd!!.fileDescriptor)
    source.channel.transferTo(0, source.channel.size(), dstination.channel)
    originFd.close()
    destinationFd.close()
    source.close()
    dstination.close()
  }

  private fun moveToFile(originFd: ParcelFileDescriptor, destinationFile:File) {
    val source = FileInputStream(originFd.fileDescriptor)
    val dstination = FileOutputStream(destinationFile)
    source.channel.transferTo(0, source.channel.size(), dstination.channel)
    originFd.close()
    source.close()
    dstination.close()
  }

  protected open fun createContentValues(metaData: MetaData): ContentValues {
    return ContentValues().apply {
      put(MediaColumns.DISPLAY_NAME, metaData.filename)
      put(MediaColumns.SIZE, metaData.size)
      put(MediaColumns.MIME_TYPE, metaData.mimeType)
      put(MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
    }
  }
}