package com.ikuzmin.scopedstorageexample.storage

import com.ikuzmin.scopedstorageexample.domain.MetaData
import com.ikuzmin.scopedstorageexample.remote.Response
import kotlinx.coroutines.flow.Flow
import java.io.FileInputStream
import java.io.InputStream

interface Storage {
  suspend fun metaData(uri:String):MetaData
  suspend fun saveFile(response: Response): Flow<SaveProgress>
  suspend fun moveToImages(uri: String,metaData: MetaData)
  suspend fun moveToDownloads(uri:String,metaData: MetaData)
}