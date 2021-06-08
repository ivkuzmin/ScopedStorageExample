package com.ikuzmin.scopedstorageexample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import com.ikuzmin.scopedstorageexample.remote.PicRepository
import com.ikuzmin.scopedstorageexample.storage.AndroidV28Storage
import com.ikuzmin.scopedstorageexample.storage.Complete
import com.ikuzmin.scopedstorageexample.storage.Intermediate
import com.ikuzmin.scopedstorageexample.storage.ScopedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
  val job = Job()
  val scope = CoroutineScope(job + Dispatchers.IO)
  lateinit var loadButton: Button
  lateinit var moveToDownloadsButton: Button
  lateinit var moveToImagesButton: Button
  lateinit var progressBar: ProgressBar
  lateinit var loadingStateTextView: TextView
  lateinit var container: View
  lateinit var uri: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val storage =
      if (BuildConfig.VERSION_CODE >= VERSION_CODES.Q) {
        ScopedStorage(this@MainActivity)
      } else {
        AndroidV28Storage(this)
      }
    loadButton = findViewById(R.id.load_button)
    progressBar = findViewById(R.id.loading_progress)
    loadingStateTextView = findViewById(R.id.loading_state_text_view)
    moveToDownloadsButton = findViewById(R.id.move_to_download_button)
    moveToImagesButton = findViewById(R.id.move_to_image_button)
    container = findViewById(R.id.container)

    loadButton.setOnClickListener {
      progressBar.visibility = View.VISIBLE
      loadingStateTextView.visibility = View.VISIBLE
      loadingStateTextView.text = getString(R.string.requested_image)
      moveToImagesButton.visibility = View.GONE
      moveToDownloadsButton.visibility = View.GONE
      moveToImagesButton.setOnClickListener {

        moveToImagesButton.isEnabled = false
        if (BuildConfig.VERSION_CODE < VERSION_CODES.Q && BuildConfig.VERSION_CODE != 1) {
          if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 300)
          } else {
            moveToImages()
          }
        } else {
          moveToImages()
        }

      }
      moveToDownloadsButton.setOnClickListener {
        moveToDownloadsButton.isEnabled = false
        if (BuildConfig.VERSION_CODE < VERSION_CODES.Q && BuildConfig.VERSION_CODE != 1) {
          if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 200)
          } else {
            moveToDownload()
          }
        } else {
          moveToDownload()
        }
      }
      loadButton.isEnabled = false
      scope.launch {
        val repo = PicRepository()
        val response = repo.loadImage(200, 200)
        storage.saveFile(response)
          .collect { progress ->
            when (progress) {
              is Intermediate -> {
                Log.d("LOADING", progress.progress().toString())
                withContext(Dispatchers.Main) {
                  loadingStateTextView.text =
                    getString(R.string.save_to_storage, progress.progress())
                  progressBar.progress = progress.progress()
                }
              }
              is Complete -> {
                Log.d("LOADING", "COMPLETE(${progress.uri()})")
                withContext(Dispatchers.Main) {
                  progressBar.visibility = View.GONE
                  progressBar.progress = 0
                  moveToImagesButton.visibility = View.VISIBLE
                  moveToDownloadsButton.visibility = View.VISIBLE
                  loadingStateTextView.visibility = View.GONE
                  loadButton.isEnabled = true
                  uri = progress.uri()
                  Snackbar.make(
                    container,
                    R.string.completed,
                    Snackbar.LENGTH_SHORT
                  ).show()
                }
              }
            }
          }
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 200 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      moveToDownload()
    } else if (requestCode == 300 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      moveToImages()
    } else {
      Snackbar.make(
        container,
        R.string.permission_denied,
        Snackbar.LENGTH_SHORT
      ).show()
      moveToDownloadsButton.isEnabled = true
      moveToImagesButton.isEnabled = true
    }
  }

  private fun moveToDownload() {
    val storage =
      if (BuildConfig.VERSION_CODE >= VERSION_CODES.Q || BuildConfig.VERSION_CODE == 1) {
        ScopedStorage(this@MainActivity)
      } else {
        AndroidV28Storage(this)
      }
    scope.launch {
      storage.moveToDownloads(uri, storage.metaData(uri))
      withContext(Dispatchers.Main) {
        moveToDownloadsButton.isEnabled = true
      }
    }
  }

  private fun moveToImages() {
    val storage =
      if (BuildConfig.VERSION_CODE >= VERSION_CODES.Q || BuildConfig.VERSION_CODE == 1) {
        ScopedStorage(this@MainActivity)
      } else {
        AndroidV28Storage(this)
      }
    scope.launch {
      storage.moveToImages(uri, storage.metaData(uri))
      withContext(Dispatchers.Main) {
        moveToImagesButton.isEnabled = true
      }
    }
  }
}