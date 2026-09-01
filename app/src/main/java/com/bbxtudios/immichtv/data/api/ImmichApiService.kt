package com.bbxtudios.immichtv.data.api

import com.bbxtudios.immichtv.data.model.AlbumResponse
import com.bbxtudios.immichtv.data.model.AssetResponse
import com.bbxtudios.immichtv.data.model.MemoryItemResponse
import com.bbxtudios.immichtv.data.model.MetadataSearchRequest
import com.bbxtudios.immichtv.data.model.RandomSearchRequest
import com.bbxtudios.immichtv.data.model.SearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ImmichApiService {

    @GET("api/view/folder/unique-paths")
    suspend fun getUniquePaths(): List<String>

    @POST("api/search/metadata")
    suspend fun searchMetadata(
        @Body request: MetadataSearchRequest
    ): SearchResponse

    @POST("api/search/random")
    suspend fun searchRandom(
        @Body request: RandomSearchRequest = RandomSearchRequest()
    ): List<AssetResponse>

    @GET("api/assets/random")
    suspend fun getRandomAssets(
        @Query("count") count: Int = 1500
    ): List<AssetResponse>

    @GET("api/assets/{id}")
    suspend fun getAssetDetail(
        @Path("id") id: String
    ): AssetResponse

    @GET("api/albums")
    suspend fun getAlbums(): List<AlbumResponse>

    @GET("api/albums/{id}")
    suspend fun getAlbumDetail(
        @Path("id") id: String,
        @Query("withoutAssets") withoutAssets: Boolean = false
    ): AlbumResponse

    @GET("api/memories")
    suspend fun getMemories(): List<MemoryItemResponse>

    @GET("api/memories/{id}")
    suspend fun getMemoryDetail(
        @Path("id") id: String
    ): MemoryItemResponse

    @GET("api/server/version")
    suspend fun getServerVersion(): Response<Unit>
}
