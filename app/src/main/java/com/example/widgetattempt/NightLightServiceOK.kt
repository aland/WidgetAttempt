package com.example.widgetattempt

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class NightLightServiceOK {
    private val TAG = "NightLightServiceOK"
    private val client = OkHttpClient()
    lateinit var data: JSONObject

    fun run() {
        val request = Request.Builder()
            .url("https://aland.themixingbowl.org/data.json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    // copy this https://github.com/juliancoronado/MinimalBitcoinWidget/blob/master/app/src/main/java/com/jcoronado/minimalbitcoinwidget/PriceWidget.kt
                    for ((name, value) in response.headers) {
                        Log.d(TAG, "Headers $name: $value")
                    }
                    val body = response.body!!.string()
                    Log.d(TAG, body)
                    //val moshi: Moshi = Moshi.Builder().build()
                    //val jsonAdapter: JsonAdapter<LocalData> = moshi.adapter<LocalData>()


                    //data = jsonAdapter.fromJson(body)!!
                    data = JSONObject(body)

                }
            }
        })
    }
}