package com.example.widgetattempt

import android.util.Log
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


class NightLightServiceOK {
    private val TAG = "NightLightServiceOK"
    private val client = OkHttpClient()

    fun run() {
        val request = Request.Builder()
            .url("https://httpbin.org/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        Log.d(TAG, "Headers $name: $value")
                    }

                    Log.d(TAG, response.body!!.string())
                }
            }
        })
    }
}