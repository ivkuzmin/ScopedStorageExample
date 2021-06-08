package com.ikuzmin.scopedstorageexample.remote

import retrofit2.Retrofit.Builder
import java.io.InputStream

class PicRepository {

  private val retrofit = Builder()
    .baseUrl("https://picsum.photos/")
    .build()
    .create(PicsumService::class.java)

  suspend fun loadImage(width: Int, height: Int): Response =
    retrofit.loadImage(width, height).let {
      val filename =
        it.headers().get("content-disposition")!!
          .substringAfterLast(";")
          .substringAfter("\"")
          .substringBefore("\"")
      val sizeBytes = if (it.headers().get("content-length") != null) {
        it.headers().get("content-length")!!.toInt()
      } else {
        it.body()!!.byteStream().available()
      }
      val contentType = it.headers().get("content-type")
      Response(filename, sizeBytes,contentType!!, it.body()!!.byteStream())
    }

}