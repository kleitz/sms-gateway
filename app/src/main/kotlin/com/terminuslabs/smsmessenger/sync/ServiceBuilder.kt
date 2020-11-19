package com.terminuslabs.smsmessenger.sync

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceBuilder {
    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://terminuslabs.com.uy/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    fun<T> buildService(service: Class<T>): T{
        return retrofit.create(service)
    }


    fun getServerUrl(ctx: Context): String? {
        val sharedPref = ctx.getSharedPreferences("SERVER_PREFERENCES", Context.MODE_PRIVATE)
        return sharedPref.getString("SERVER_URL", "")
    }

    fun getAuthHeader(ctx: Context): String? {
        val sharedPref = ctx.getSharedPreferences("SERVER_PREFERENCES", Context.MODE_PRIVATE)
        return sharedPref.getString("SERVER_AUTH", "")
    }

    fun setUrl(ctx: Context, url: String){
        val sharedPref = ctx.getSharedPreferences("SERVER_PREFERENCES", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("SERVER_URL", url)
            commit()
        }
    }

    fun setAuthHeader(ctx: Context, auth: String){
        val sharedPref = ctx.getSharedPreferences("SERVER_PREFERENCES", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("SERVER_AUTH", auth)
            commit()
        }
    }



}
