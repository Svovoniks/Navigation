package com.svovo.navigation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.StringRequest
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONObject
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.net.URLEncoder

class Utils {
    val LOCATION_RQ = 100

    fun checkLocationPermission(context: Activity): Boolean{
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
        {
            ActivityCompat.requestPermissions(context, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_RQ)

            return false
        }
        return true
    }

    fun centerMap(location: Location, map: MapView){
        map.controller.animateTo(GeoPoint(location))
    }

    fun setPositionMarker(marker: Marker, location: Location){
        marker.position = GeoPoint(location)
    }

}

class PathManager(map: MapView, onBackPressedDispatcher: OnBackPressedDispatcher) : OnBackPressedCallback(true) {
    private var pathList: ArrayList<Polyline>
    private val map: MapView
    private val startMarker: Marker
    private val destMarker: Marker
    private val onBackPressedDispatcher: OnBackPressedDispatcher
    init{
        this.map = map
        this.onBackPressedDispatcher = onBackPressedDispatcher
        startMarker = Marker(map)
        destMarker = Marker(map)
        pathList = ArrayList()
    }

    private fun clearPaths(){
        for (path in pathList){
            map.overlays.remove(path)
        }
        pathList = ArrayList()
        map.invalidate()
    }

    private fun clearMarkers(){
        map.overlays.remove(startMarker)
        map.overlays.remove(destMarker)
        map.invalidate()
    }
    private fun setMarker(marker: Marker, point: GeoPoint, tryBuild: Boolean = true){
        marker.position = point
        map.overlays.add(marker)
        map.invalidate()
        if (tryBuild){
            tryBuildRoute()
        }
        isEnabled = true
        onBackPressedDispatcher.addCallback(this)
    }

    private fun canBuildPath(): Boolean{
        return map.overlays.contains(startMarker) && map.overlays.contains(destMarker)
    }

    fun setStart(point: GeoPoint){
        map.overlays.remove(startMarker)
        setMarker(startMarker, point)
    }

    fun setDest(point: GeoPoint){
        map.overlays.remove(destMarker)
        setMarker(destMarker, point)
    }

    private fun tryBuildRoute(){
        if (canBuildPath()){
            clearPaths()
            RoutingServer().getRoute(
                startMarker.position,
                destMarker.position,
                {jsonObject ->  plotPaths(jsonObject)},
                { Toast.makeText(map.context, "Couldn't get a route", Toast.LENGTH_LONG).show() }
            )
        }
    }

    private fun plotPaths(jsonObject: JSONObject){
        clearPaths()

        val arr = jsonObject.getJSONObject("route").getJSONArray("paths")
        for (i in 0..<arr.length()){
            val polyline = Polyline(map)

            polyline.addPoint(startMarker.position)

            val snapped = arr.getJSONObject(i).getJSONObject("snapped_waypoints").getJSONArray("coordinates")
            polyline.addPoint(GeoPoint(
                snapped.getJSONArray(0).getDouble(1),
                snapped.getJSONArray(0).getDouble(0))
            )

            val points = arr.getJSONObject(i).getJSONObject("points").getJSONArray("coordinates")
            for (i in 0..<points.length()){
                val x =  points.getJSONArray(i).getDouble(0)
                val y =  points.getJSONArray(i).getDouble(1)
                polyline.addPoint(GeoPoint(y, x))
                Log.d("building path", "$y, $x")

            }

            polyline.addPoint(GeoPoint(
                snapped.getJSONArray(1).getDouble(1),
                snapped.getJSONArray(1).getDouble(0))
            )

            polyline.addPoint(destMarker.position)

            pathList.add(polyline)
            map.overlays.add(polyline)
        }
        clearMarkers()

        setMarker(startMarker, startMarker.position, false)
        setMarker(destMarker, destMarker.position, false)
        map.invalidate()
        isEnabled = true
        onBackPressedDispatcher.addCallback(this)
    }

    override fun handleOnBackPressed() {
        clearPaths()
        clearMarkers()
        remove()
    }
}

class MapEventsReceiverImpl(map: MapView,
                            pathManager: PathManager,
                            routingLayout: ConstraintLayout,
                            fromButton: ExtendedFloatingActionButton,
                            toButton: ExtendedFloatingActionButton,
                            onBackPressedDispatcher: OnBackPressedDispatcher) : MapEventsReceiver, OnBackPressedCallback(true) {
    private val selectedMarker: Marker
    private val map: MapView
    private val pathManager: PathManager
    private val routingLayout: ConstraintLayout
    private val fromButton: ExtendedFloatingActionButton
    private val toButton: ExtendedFloatingActionButton
    private val onBackPressedDispatcher: OnBackPressedDispatcher

    init {
        this.map = map
        this.routingLayout = routingLayout
        this.fromButton = fromButton
        this.toButton = toButton
        this.pathManager = pathManager
        this.onBackPressedDispatcher = onBackPressedDispatcher

        selectedMarker = Marker(map)

        fromButton.setOnClickListener { updatePoint(pathManager::setStart) }
        toButton.setOnClickListener { updatePoint(pathManager::setDest) }
    }

    private fun updatePoint(update: (GeoPoint) -> Unit){
        update(selectedMarker.position)
        removeSelectMarker()
    }

    private fun placeSelectMarker(point: GeoPoint){
        selectedMarker.position = point
        map.overlays.add(selectedMarker)
        routingLayout.visibility = ConstraintLayout.VISIBLE
        map.invalidate()
        isEnabled = true
        onBackPressedDispatcher.addCallback(this)
    }

    private fun removeSelectMarker(){
        map.overlays.remove(selectedMarker)
        routingLayout.visibility = ConstraintLayout.INVISIBLE
        map.invalidate()
        remove()
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        Log.d("singleTapConfirmedHelper", "${p?.latitude} - ${p?.longitude}")
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        Log.d("longPressHelper", "${p?.latitude} - ${p?.longitude}")
        if (p != null) {
            placeSelectMarker(p)
        }
        return false
    }

    override fun handleOnBackPressed() {
        removeSelectMarker()
    }
}

class Search (context: Activity){
    private val markerList: ArrayList<Marker> = ArrayList()
    private val searchObjectList: ArrayList<SearchObject> = ArrayList()
    private val context: Activity

    init {
        this.context = context
    }

    fun executeForAllFound(action: (ArrayList<SearchObject>) -> Unit){
        action(searchObjectList)
    }


    fun drawAll(map: MapView){
        for (i in searchObjectList){
            val mk = Marker(map)
            mk.position = i.point
            map.overlays.add(mk)
            markerList.add(mk)
        }
        map.invalidate()
    }

    fun removeAllMarkers(map: MapView){
        for (i in markerList){
            map.overlays.remove(i)
        }
        markerList.clear()
    }

    fun clearSearchList(){
        searchObjectList.clear()
    }

    fun find(name: String, map: MapView, action: (ArrayList<SearchObject>) -> Unit) {
        val cache = DiskBasedCache(File("cache"), 1024 * 1024)
        val network = BasicNetwork(HurlStack())
        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }

        var url = "https://nominatim.openstreetmap.org/search.php?format=json&accept-language=en&addressdetails=1&limit=50&q="

        url += URLEncoder.encode(name, "UTF-8")

        val point1 = map.projection.northEast
        val point2 = map.projection.southWest

        url += "&viewbox=" + point1.longitude + "," + point1.latitude+ "," + point2.longitude + "," + point2.latitude
        class CustomRequest : StringRequest(
            Method.GET, url,
            Response.Listener { response ->

                val parsed = JsonParser.parseString(response)
                if (!parsed.isJsonArray){
                    Toast.makeText(context, "doesn't exist", Toast.LENGTH_LONG).show()
                    return@Listener
                }

                for (i in parsed.asJsonArray){
                    val k = i.asJsonObject
                    searchObjectList.add(SearchObject(GeoPoint(k["lat"].asDouble, k["lon"].asDouble), k))
                }
                action(searchObjectList)
            },
            Response.ErrorListener { error -> Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show()}) {
            override fun getHeaders(): MutableMap<String, String> {
                val map = HashMap<String, String>()
                map["User-Agent"] = "Mozilla/5.0"
                return map
            }
        }
        requestQueue.add(CustomRequest())
    }
}

class SearchObject(point: GeoPoint, fullData: JsonObject){
    val point: GeoPoint
    val fullData: JsonObject
    init {
        this.point = point
        this.fullData = fullData
    }
}

