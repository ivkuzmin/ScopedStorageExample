package com.ikuzmin.scopedstorageexample.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.InputStream

interface PicsumService {

  @GET("{width}/{height}")
  suspend fun loadImage(@Path("width") width: Int, @Path("height") height: Int): Response<ResponseBody>
}