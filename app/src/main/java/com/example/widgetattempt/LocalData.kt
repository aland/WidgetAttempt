package com.example.widgetattempt

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LocalData(val light: Int, val moonPct: Double)