package com.hudsom.kotlinceapp.api

import com.hudsom.kotlinceapp.model.Comunidade
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface FirebaseApi {

    @GET("comunidades.json")
    suspend fun listarComunidades(): Response<Map<String, Comunidade>?>

    @PUT("comunidades/{id}.json")
    suspend fun salvarComunidade(@Path("id") id: String, @Body comunidade: Comunidade): Response<Comunidade>

    @DELETE("comunidades/{id}.json")
    suspend fun excluirComunidade(@Path("id") id: String): Response<Void>
}
