package com.example.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {
  @GET("rest/v1/nurses")
  suspend fun getAllNurses(
    @Query("select") select: String = "*",
    @Query("order") order: String = "created_at.desc",
  ): List<NurseRecord>

  @Headers("Prefer: resolution=merge-duplicates,return=representation")
  @POST("rest/v1/nurses?on_conflict=phone")
  suspend fun upsertNurse(@Body record: NurseRecord): List<NurseRecord>

  @DELETE("rest/v1/nurses")
  suspend fun deleteNurse(@Query("phone") phoneEq: String)
}
