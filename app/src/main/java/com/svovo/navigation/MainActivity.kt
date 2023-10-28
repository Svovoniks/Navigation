package com.svovo.navigation

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import android.view.MotionEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.svovo.navigation.databinding.ActivityMainBinding
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.view.ScaleGestureDetector



class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var map : MapView
    private lateinit var locationManager: LocationManager
    private lateinit var currentLocation: Location
    private val utils = Utils()
    private lateinit var locationMarker: Marker
    private lateinit var centerMapFab: FloatingActionButton
    private lateinit var scaleGestureDetector: ScaleGestureDetector


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        centerMapFab = findViewById(R.id.center_map_fab)
        findViewById<FloatingActionButton>(R.id.zoom_in_fab).setOnClickListener {
            if (map.canZoomIn()) map.controller.zoomTo(map.zoomLevelDouble + 1)
        }

        findViewById<FloatingActionButton>(R.id.zoom_out_fab).setOnClickListener {
            if (map.canZoomOut()) map.controller.zoomTo(map.zoomLevelDouble - 1)
        }

        Configuration.getInstance().load(this, getSharedPreferences("mapInit", Context.MODE_PRIVATE))

        map = findViewById<MapView>(R.id.map)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.setTileSource(TileSourceFactory.MAPNIK)
        locationMarker = Marker(map)
        map.overlays.add(locationMarker)

        val mapController = map.controller
        mapController.setZoom(18.4)

        setupLocation()

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                // Можно оставить пустым или выполнить необходимые действия перед началом масштабирования
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentSpan = detector.currentSpan
                val previousSpan = detector.previousSpan
                if (currentSpan > previousSpan) {
                    // Масштабирование карты в случае расширения двумя пальцами
                    if (map.canZoomOut()) map.controller.zoomTo(map.zoomLevelDouble + 1)
                } else {
                    // Масштабирование карты в случае сжатия двумя пальцами
                    if (map.canZoomIn()) map.controller.zoomTo(map.zoomLevelDouble - 1)
                }
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    private fun setupLocation() {
        if (utils.checkLocationPermission(this)){
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    if (location != null) {
                        currentLocation = location
                        utils.centerMap(location, map)
                        utils.setPositionMarker(locationMarker, location)
                    }
                }
            centerMapFab.setOnClickListener { if (::currentLocation.isInitialized) utils.centerMap(currentLocation, map) }
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }

    }
    override fun onLocationChanged(location: Location) {
        currentLocation = location
        utils.setPositionMarker(locationMarker, location)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == utils.LOCATION_RQ) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                grantResults.size > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                setupLocation()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                locationMarker.setVisible(false)
                map.invalidate()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            scaleGestureDetector.onTouchEvent(it)
        }
        return super.onTouchEvent(event)
    }
}