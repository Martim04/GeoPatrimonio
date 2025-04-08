    package com.example.projeto

    import android.Manifest
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Bundle
    import android.widget.Button
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.activity.viewModels
    import androidx.appcompat.app.AppCompatActivity
    import androidx.appcompat.widget.Toolbar
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.core.view.GravityCompat
    import androidx.drawerlayout.widget.DrawerLayout
    import com.google.android.gms.maps.CameraUpdateFactory
    import com.google.android.gms.maps.GoogleMap
    import com.google.android.gms.maps.OnMapReadyCallback
    import com.google.android.gms.maps.SupportMapFragment
    import com.google.android.gms.maps.model.LatLng
    import com.google.android.gms.maps.model.MapStyleOptions
    import com.google.android.material.navigation.NavigationView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import com.example.projeto.FavoriteEventsFragment
    class MainActivity : AppCompatActivity(), OnMapReadyCallback {

        private lateinit var drawerLayout: DrawerLayout
        private lateinit var navigationView: NavigationView
        private lateinit var toolbar: Toolbar
        private lateinit var zoomInButton: Button
        private lateinit var zoomOutButton: Button
        private lateinit var navIcon: ImageView
        private lateinit var db: FirebaseFirestore
        private lateinit var auth: FirebaseAuth
        private val zoomViewModel: ZoomViewModel by viewModels()
        private lateinit var googleMap: GoogleMap

        companion object {
            private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        }

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

            navIcon = findViewById(R.id.nav_icon)

            // Set Toolbar as ActionBar
            setSupportActionBar(toolbar)

            // Set navIcon click listener to open the drawer
            navIcon.setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }

            // Set navigation item selected listener
            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_home -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, HomeFragment())
                            .commit()
                    }
                    R.id.nav_profile -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, ProfileFragment())
                            .commit()
                    }
                    R.id.nav_settings -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, SettingsFragment())
                            .commit()
                    }
                    R.id.nav_pois -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, POIMapFragment())
                            .commit()
                    }
                    R.id.nav_add_poi -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, AddPoiFragment())
                            .commit()
                    }
                    R.id.nav_favorite_events -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, FavoriteEventsFragment())
                            .commit()
                    }
                    R.id.nav_update_poi -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, UpdatePoiFragment())
                            .commit()
                    }
                    R.id.nav_add_event -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, AddEventFragment())
                            .commit()
                    }
                    R.id.nav_admin -> {
                        // Handle admin navigation
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, AdminFragment())
                            .commit()
                    }
                    R.id.nav_logout -> {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }
                    R.id.nav_favorites -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, FavoritePoisFragment())
                            .commit()
                    }
                }
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }

            // Load SupportMapFragment by default
            if (savedInstanceState == null) {
                val mapFragment = SupportMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, mapFragment)
                    .commit()
                mapFragment.getMapAsync(this)
            }

            // Set zoom button listeners


            // Fetch and display user details in the navigation header
            val headerView = navigationView.getHeaderView(0)
            val navHeaderName = headerView.findViewById<TextView>(R.id.nav_header_name)
            val navHeaderEmail = headerView.findViewById<TextView>(R.id.nav_header_email)
            val currentUser = auth.currentUser

            currentUser?.let {
                db.collection("users").document(it.uid).get().addOnSuccessListener { document ->
                    if (document != null) {
                        navHeaderName.text = document.getString("name")
                        navHeaderEmail.text = document.getString("email")
                        val userType = document.getString("type")
                        if (userType == "admin") {
                            navigationView.menu.findItem(R.id.nav_add_event).isVisible = true
                            navigationView.menu.findItem(R.id.nav_update_poi).isVisible = true
                            navigationView.menu.findItem(R.id.nav_admin).isVisible = true

                        }
                    }
                }
            }
        }

        override fun onMapReady(map: GoogleMap) {
            googleMap = map
            val style = """
    [
        {
            "featureType": "poi.business",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        },
        {
            "featureType": "poi.attraction",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        },
        {
            "featureType": "poi.government",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        },
        {
            "featureType": "poi.medical",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        },
        {
            "featureType": "poi.park",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        },
        {
            "featureType": "poi.place_of_worship",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        },
        {
            "featureType": "poi.school",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        },
        {
            "featureType": "poi.sports_complex",
            "elementType": "labels",
            "stylers": [{"visibility": "off"}]
        }
    ]
""".trimIndent()
            googleMap.setMapStyle(MapStyleOptions(style));
            val porto = LatLng(41.14961, -8.61099)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(porto, 15f))
            enableMyLocation()
            zoomViewModel.setGoogleMap(googleMap)
            googleMap.uiSettings.isZoomControlsEnabled = true
        }

        private fun enableMyLocation() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.isMyLocationEnabled = true
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                }
            }
        }
    }