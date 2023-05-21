package com.example.widgetattempt

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.material.tabs.TabLayout.TabIndicatorGravity
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException

private const val TAG = "widgetattempt"

/**
 * Implementation of App Widget functionality.
 */
class NewAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        /*If your widget setup process can take several seconds (perhaps while performing web requests) and you require that
your process continues, consider starting a Task using WorkManager in the onUpdate() method.
From within the task, you can perform your own updates to the widget without worrying about the AppWidgetProvider closing down
due to an Application Not Responding (ANR) error.*/
        // Other doc says WorkManager is not right, and Coroutines would be better suited
        // https://developer.android.com/kotlin/coroutines
        // but using okhttp probably doesn't need coroutines, so just use that?

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive " + intent?.action)
    }

}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    Log.d(TAG, "updateAppWidget")

    // won't this be synchronous in this context (although the service is implemented with callback?)
    //val nightLight = NightLightServiceOK().run()
    //Log.d(TAG, "a" + "b")
    //Log.d(TAG, NightLightServiceOK().data.getInt("light").toString()
    //    + " " + NightLightServiceOK().data.getDouble("moonPct").toString())

    val widgetText = context.getString(R.string.loading_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    views.setTextViewText(R.id.appwidget_text, widgetText)

    // first update call to set loading text
    appWidgetManager.updateAppWidget(appWidgetId, views) // continues after this

    val intent = Intent(context, NewAppWidget::class.java)
        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
    val pendingIntent = PendingIntent.getBroadcast(
        context, appWidgetId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    views.setOnClickPendingIntent(R.id.appwidget_button, pendingIntent)

    // fetch location data from phone if granted
    fetchLocation(appWidgetManager, appWidgetId, views, context)

    // function call to fetch data from HTTP GET request
    fetchData(appWidgetManager, appWidgetId, views, context)
}
fun fetchLocation (
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    views: RemoteViews,
    context: Context
) {
    /*
    ActivityCompat.requestPermissions()
    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            } else -> {
            // No location access granted.
        }
        }
    }
     */
    var accessFineLocation = false
    var accessCoarseLocation = false
    if (ActivityCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Fine location available")
        accessFineLocation = true
    }
    if (ActivityCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Coarse location available")
        accessCoarseLocation = true
    }

    if (!accessFineLocation && !accessCoarseLocation) {
        Log.d(TAG, "No location permission granted")
        /*
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
         */
        return
    }


    var currentLocation: Location? = null
    val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var locationByGps: Location? = null
    var locationByNetwork: Location? = null
    val gpsLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationByGps= location
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
//------------------------------------------------------//
    val networkLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationByNetwork= location
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    val lastKnownLocationByGps =
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    lastKnownLocationByGps?.let {
        locationByGps = lastKnownLocationByGps
    }
//------------------------------------------------------//
    val lastKnownLocationByNetwork =
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    lastKnownLocationByNetwork?.let {
        locationByNetwork = lastKnownLocationByNetwork
    }
//------------------------------------------------------//
    if (locationByGps != null && locationByNetwork != null) {
        Log.d(TAG, "Either location by GPS or network is available")
        Log.d(TAG, "Gps accuracy: " + locationByGps!!.accuracy.toString())
        Log.d(TAG, "Network accuracy: " + locationByNetwork!!.accuracy.toString())
        if (locationByGps!!.accuracy > locationByNetwork!!.accuracy) {
            currentLocation = locationByGps
            val latitude = currentLocation?.latitude
            val longitude = currentLocation?.longitude
            // use latitude and longitude as per your need
            Log.i(TAG, "locationByGps lat: $latitude, long: $longitude")
        } else {
            currentLocation = locationByNetwork
            val latitude = currentLocation?.latitude
            val longitude = currentLocation?.longitude
            // use latitude and longitude as per your need
            Log.i(TAG, "locationByNetwork lat: $latitude, long: $longitude")
        }
    }
    else {
        Log.d(TAG, "Neither location by GPS nor network is available")
    }
}
fun fetchData(
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    views: RemoteViews,
    context: Context
) {

    val url = "https://aland.themixingbowl.org/data.json"

    val request = Request.Builder().url(url).build()

    val client = OkHttpClient()


    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            // successful GET request
            Log.i(TAG, "GET request successful.")

            // converts response into string
            val body = response.body?.string()

            val data: LocalData = Gson().fromJson(body, LocalData::class.java)

            Log.i(TAG, data.moonPct.toString())
            Log.i(TAG, data.light.toString())

            val moonPct = data.moonPct * 100

            views.setTextViewText(R.id.appwidget_text,
                moonPct.toString() + "% " + data.light.toString()
            )

            // makes final call to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        override fun onFailure(call: Call, e: IOException) {
            // failed GET request
            Log.i(TAG, "Failed to execute GET request.")
        }
    })

}