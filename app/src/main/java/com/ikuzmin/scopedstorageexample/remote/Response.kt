package com.ikuzmin.scopedstorageexample.remote

import java.io.InputStream

data class Response(
  val filename: String,
  val size: Int,
  val contentType: String,
  val content: InputStream
)
