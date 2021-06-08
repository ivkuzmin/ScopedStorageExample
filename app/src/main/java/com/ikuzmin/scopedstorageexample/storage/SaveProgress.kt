package com.ikuzmin.scopedstorageexample.storage

sealed class SaveProgress

class Intermediate(private val progress: Int) : SaveProgress() {

  fun progress(): Int = progress

}

class Complete(private val uri: String) : SaveProgress() {
  fun uri(): String = uri
}

