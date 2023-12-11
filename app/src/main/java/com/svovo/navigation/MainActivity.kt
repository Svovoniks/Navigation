package com.svovo.navigation

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.svovo.navigation.databinding.ActivityMainBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var map : MapView
    private lateinit var locationManager: LocationManager
    private lateinit var currentLocation: Location
    private val utils = Utils()
    private lateinit var locationMarker: Marker
    private lateinit var centerMapFab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        //setSupportActionBar(binding.toolbar)

        //val navController = findNavController(R.id.nav_host_fragment_content_main)
        //appBarConfiguration = AppBarConfiguration(navController.graph)
        //setupActionBarWithNavController(navController, appBarConfiguration)

        centerMapFab = findViewById(R.id.center_map_fab)
        findViewById<FloatingActionButton>(R.id.zoom_in_fab).setOnClickListener {
            if (map.canZoomIn()) map.controller.zoomTo(map.zoomLevelDouble + 2)
        }

        val search = Search(this)

        findViewById<FloatingActionButton>(R.id.zoom_out_fab).setOnClickListener {
            if (map.canZoomOut()) map.controller.zoomTo(map.zoomLevelDouble - 2)
        }

        findViewById<TextInputEditText>(R.id.search_bar_edit_text).setOnKeyListener(View.OnKeyListener { v, i, key ->
            if (key.action == KeyEvent.ACTION_DOWN && key.keyCode == KeyEvent.KEYCODE_ENTER) {
                search.clearSearchList()
                search.removeAllMarkers(map)
                search.find((v as TextInputEditText).text.toString(), map) { _ -> search.drawAll(map) }
                return@OnKeyListener true
            }
            return@OnKeyListener false
        })

        Configuration.getInstance().load(this, getSharedPreferences("mapInit", Context.MODE_PRIVATE))

        map = findViewById(R.id.map)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.setTileSource(TileSourceFactory.MAPNIK)
        locationMarker = Marker(map)

        val d = getDrawable(R.drawable.marker_icon)
        val bitmap = (d as BitmapDrawable).bitmap
        val dr: Drawable = BitmapDrawable(
            resources,
            Bitmap.createScaledBitmap(
                bitmap,
                (30.0f * resources.displayMetrics.density).toInt(),
                (30.0f * resources.displayMetrics.density).toInt(),
                true
            )
        )
        locationMarker.icon = dr
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        map.overlays.add(locationMarker)

        val pathManager = PathManager(map, onBackPressedDispatcher)

        val eventsReceiver = MapEventsReceiverImpl(map,
            pathManager,
            findViewById(R.id.routing_layout),
            findViewById(R.id.from_fab),
            findViewById(R.id.to_fab),
            onBackPressedDispatcher
        )
        map.overlays.add(MapEventsOverlay(eventsReceiver))

        map.setMultiTouchControls(true)
        map.overlays.add(RotationGestureOverlay(map))

        val mapController = map.controller
        mapController.setZoom(18.4)

        setupLocation()

        binding.bottomNav.selectedItemId = R.id.mapFragment

        binding.bottomNav.itemIconTintList = null

        var mapFlag = true

        binding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.mapFragment -> {
                    if(!mapFlag) {
                        removeFragment()
                        mapFlag = true
                    }
                }
                R.id.discover -> {
                    loadFragment(DiscoverFragment())
                    mapFlag = false
                }
                R.id.profile -> {
                    if (loggedIn) {
                        loadFragment(ProfileFragment())
                    } else if (loginPageLoaded) loadFragment(LoginFragment())
                    else loadFragment(SignUpFragment())
                    mapFlag = false
                }
            }
            true
        }
        onBackPressedDispatcher.addCallback(this.callback)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, fragment, fragment.javaClass.simpleName)
            .commit()
    }

    private fun removeFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        fragment?.let {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.remove(it).commitAllowingStateLoss()
        }
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.bottomNav.selectedItemId == R.id.mapFragment) {
                finishAffinity()
            } else {
                binding.bottomNav.selectedItemId = R.id.mapFragment
            }
        }
    }


//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.mapFragment -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }


    private fun setupLocation() {
        if (utils.checkLocationPermission(this)){
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    if (location != null) {
                        map.controller.setCenter(GeoPoint(location))
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

    companion object {
        var loggedIn: Boolean = false
        var loginPageLoaded : Boolean = true
        var signupPageLoaded : Boolean = false
    }
}