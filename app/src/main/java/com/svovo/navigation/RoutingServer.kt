package com.svovo.navigation

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.File

class RoutingServer {
    private val rootURL: String = "https://gradually-quality-mole.ngrok-free.app"
    private val routingURL: String = "get-route/{startLat}/{startLon}/{destLat}/{destLon}?start_lat=%s&start_lon=%s&dest_lat=%s&dest_lon=%s"
    private val requestQueue: RequestQueue

    init {
        val cache = DiskBasedCache(File("cache"), 1024 * 1024)
        val network = BasicNetwork(HurlStack())
        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }
        this.requestQueue = requestQueue;
    }
    fun getRoute(start: GeoPoint, dest: GeoPoint, onResponse: (JSONObject) -> Unit, onError: (VolleyError) -> Unit){
        val urlList = listOf(
            rootURL,
            routingURL.format(start.longitude.toString(), start.latitude.toString(), dest.longitude.toString(), dest.latitude.toString())
        )
        val url = urlList.joinToString(separator = "/")
        Log.d("getting route", url)
        val request = JsonObjectRequest(Request.Method.GET, url, null,
            {response -> onResponse(response)},
            {error -> onError(error)}
        )
        requestQueue.add(request)
    }
}