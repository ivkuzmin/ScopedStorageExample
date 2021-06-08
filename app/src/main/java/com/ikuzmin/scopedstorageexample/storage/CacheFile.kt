package com.ikuzmin.scopedstorageexample.storage

import android.content.Context
import java.io.File

class CacheFile(private val context: Context, private val filename: String) {
  fun create(): File {
    val path = File(context.getExternalFilesDir(null), "images").also {
      it.mkdir()
    }
    return File(path, filename).also {
      it.createNewFile()
    }
  }
}