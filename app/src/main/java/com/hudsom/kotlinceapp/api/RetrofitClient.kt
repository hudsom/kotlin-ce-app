package com.hudsom.kotlinceapp.api

import com.hudsom.kotlinceapp.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    val instancia: FirebaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.FIREBASE_DATABASE_URL + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FirebaseApi::class.java)
    }
}
