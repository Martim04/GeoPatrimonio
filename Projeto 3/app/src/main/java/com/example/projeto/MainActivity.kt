package com.example.projeto

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var zoomInButton: Button
    private lateinit var zoomOutButton: Button
    private lateinit var navIcon: ImageView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        zoomInButton = findViewById(R.id.zoomInButton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        navIcon = findViewById(R.id.nav_icon)

        // Set Toolbar as ActionBar
        setSupportActionBar(toolbar)

        // Set navigation icon click listener
        navIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set navigation item selected listener
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Handle home navigation
                }
                R.id.nav_profile -> {
                    // Handle profile navigation
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Set user info in nav header
        val headerView = navigationView.getHeaderView(0)
        val navHeaderName = headerView.findViewById<TextView>(R.id.nav_header_name)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.nav_header_email)

        val user = auth.currentUser
        user?.let {
            navHeaderEmail.text = it.email
            db.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        navHeaderName.text = document.getString("name")
                    }
                }
                .addOnFailureListener { e ->
                    // Handle the error
                }
        }

        // Get the SupportMapFragment and request notification when the map is ready to be used
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set zoom button listeners
        zoomInButton.setOnClickListener {
            zoomIn()
        }

        zoomOutButton.setOnClickListener {
            zoomOut()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Get the current location
            getDeviceLocation()
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getDeviceLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    // Move the camera to the current location
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    // Enable the location button
                    mMap.isMyLocationEnabled = true
                }
            }
        } catch (e: SecurityException) {
            // Handle security exception
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get the current location
                getDeviceLocation()
            } else {
                // Permission denied, show a message or handle differently
            }
        }
    }

    private fun zoomIn() {
        val currentZoomLevel = mMap.cameraPosition.zoom
        mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel + 1))
    }

    private fun zoomOut() {
        val currentZoomLevel = mMap.cameraPosition.zoom
        mMap.animateCamera(CameraUpdateFactory.zoomTo(currentZoomLevel - 1))
    }
}