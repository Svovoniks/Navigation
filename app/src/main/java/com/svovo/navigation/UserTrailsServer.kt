package com.svovo.navigation

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.File

class UserTrailsServer {
    private val rootURL: String = "https://gradually-quality-mole.ngrok-free.app"
    private val getTrailsURL: String = "get-trails/"
    private val addTrailURL: String = "add-trail/"
    private val requestQueue: RequestQueue

    init {
        val cache = DiskBasedCache(File("cache"), 1024 * 1024)
        val network = BasicNetwork(HurlStack())
        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }
        this.requestQueue = requestQueue;
    }
    fun getTrails(sessionKey: String, onResponse: (ArrayList<Trail>) -> Unit, onError: (VolleyError) -> Unit) {
        val url = listOf(rootURL, getTrailsURL).joinToString(separator = "/")
        val requestJSON = JSONObject()
        requestJSON.put("session_key", sessionKey)

        val request = JsonObjectRequest(
            Request.Method.POST, url, requestJSON,
            {response -> onResponse(parseTrails(response))},
            {error -> onError(error)}
        )
        requestQueue.add(request)
    }

    fun addTrail(sessionKey: String, trail: Trail, onResponse: () -> Unit, onError: (VolleyError) -> Unit){
        val url = listOf(rootURL, addTrailURL).joinToString(separator = "/")
        val requestJSON = JSONObject()
        requestJSON.put("authenticated", true)
        requestJSON.put("session_key", sessionKey)
        requestJSON.put("trails", packPoints(trail))

        val request = JsonObjectRequest(
            Request.Method.POST, url, requestJSON,
            {response -> onResponse()},
            {error -> onError(error)}
        )
        requestQueue.add(request)
    }

    private fun parseTrails(json: JSONObject): ArrayList<Trail> {
        if (!json.getBoolean("authenticated")){
            return ArrayList()
        }

        val trails = ArrayList<Trail>()

        val arr = json.getJSONArray("trails")
        for (i in 0..<arr.length()){
            val trail = arr.getJSONObject(i)
            trails.add(Trail(trail.getLong("id"), trail.getString("name"), parsePoints(trail.getJSONArray("points"))))
        }

        return trails
    }

    private fun parsePoints(jsonArray: JSONArray) : ArrayList<GeoPoint>{
        val result = ArrayList<GeoPoint>()
        for (i in 0..<jsonArray.length()){
            val point = jsonArray.getJSONObject(i)
            result.add(GeoPoint(point.getDouble("x"), point.getDouble("y")))
        }

        return result
    }

    private fun packPoints(trail: Trail) : JSONArray{
        val result = JSONArray()

        val trailJSON = JSONObject()
        trailJSON.put("name", trail.name)

        val pointArr = JSONArray()
        for (i in trail.points){
            val point = JSONObject()
            point.put("x", i.latitude)
            point.put("y", i.longitude)
            pointArr.put(point)
        }
        trailJSON.put("points", pointArr)

        result.put(trailJSON)
        return result
    }
}