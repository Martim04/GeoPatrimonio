package com.example.projeto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.Dialog

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var navIcon: ImageView
    private lateinit var searchView: SearchView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val zoomViewModel: ZoomViewModel by viewModels()
    private lateinit var googleMap: GoogleMap
    private val poiMarkers = mutableMapOf<Marker, Map<String, Any>>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MainActivity"
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
        searchView = findViewById(R.id.search_view)

        // Set Toolbar as ActionBar
        setSupportActionBar(toolbar)

        // Set click listener for navigation icon
        navIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Find the GeoPatrimonio TextView dynamically and set click listener
        val titleTextView = findGeoPatrimonioTextView(toolbar)
        titleTextView?.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.map_container)
            if (currentFragment !is SupportMapFragment) {
                val mapFragment = SupportMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.map_container, mapFragment, "MAP_FRAGMENT")
                    .addToBackStack(null)
                    .commit()
                mapFragment.getMapAsync(this)
                searchView.visibility = View.VISIBLE
            }
        }

        // Set navigation item click listener
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                R.id.nav_pois -> replaceFragment(POIMapFragment())
                R.id.nav_add_poi -> replaceFragment(AddPoiFragment())
                R.id.nav_favorite_events -> replaceFragment(FavoriteEventsFragment())
                R.id.nav_update_poi -> replaceFragment(UpdatePoiFragment())
                R.id.nav_add_event -> replaceFragment(AddEventFragment())
                R.id.nav_admin -> replaceFragment(AdminFragment())
                R.id.nav_logout -> logoutUser()
                R.id.nav_favorites -> replaceFragment(FavoritePoisFragment())
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Load map fragment by default and show SearchView
        if (savedInstanceState == null) {
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.map_container, mapFragment, "MAP_FRAGMENT")
                .commit()
            mapFragment.getMapAsync(this)
            searchView.visibility = View.VISIBLE
        }

        // Set up navigation header
        setupNavigationHeader()

        // Set up SearchView
        setupSearchView()
    }

    private fun findGeoPatrimonioTextView(toolbar: Toolbar): TextView? {
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is ViewGroup) {
                for (j in 0 until child.childCount) {
                    val grandChild = child.getChildAt(j)
                    if (grandChild is TextView && grandChild.text == "GeoPatrimonio") {
                        return grandChild
                    }
                }
            }
        }
        return null
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .addToBackStack(null)
            .commit()

        // Show SearchView only for SupportMapFragment
        searchView.visibility = if (fragment is SupportMapFragment) View.VISIBLE else View.GONE
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupNavigationHeader() {
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
        configureMapStyle()
        val porto = LatLng(41.14961, -8.61099)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(porto, 15f))
        enableMyLocation()
        zoomViewModel.setGoogleMap(googleMap)
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Load public POIs
        loadPublicPOIs()

        // Set marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            showPOIDetailsPopup(marker)
            true
        }
    }

    private fun configureMapStyle() {
        val style = """
        [
            {"featureType": "poi.business", "elementType": "labels", "stylers": [{"visibility": "off"}]},
            {"featureType": "poi.attraction", "elementType": "labels", "stylers": [{"visibility": "off"}]},
            {"featureType": "poi.government", "elementType": "labels", "stylers": [{"visibility": "off"}]},
            {"featureType": "poi.medical", "elementType": "labels", "stylers": [{"visibility": "off"}]},
            {"featureType": "poi.park", "elementType": "labels", "stylers": [{"visibility": "off"}]},
            {"featureType": "poi.place_of_worship", "elementType": "labels", "stylers": [{"visibility": "off"}]},
            {"featureType": "poi.school", "elementType": "labels", "stylers": [{"visibility": "off"}]},
            {"featureType": "poi.sports_complex", "elementType": "labels", "stylers": [{"visibility": "off"}]}
        ]
        """.trimIndent()
        googleMap.setMapStyle(MapStyleOptions(style))
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchPOIs(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { searchPOIs(it) }
                return true
            }
        })
    }

    private fun loadPublicPOIs() {
        db.collection("POIs")
            .whereEqualTo("publico", true)
            .get()
            .addOnSuccessListener { result ->
                googleMap.clear()
                poiMarkers.clear()
                for (document in result) {
                    val lat = document.getDouble("latitude") ?: continue
                    val lng = document.getDouble("longitude") ?: continue
                    val title = document.getString("titulo") ?: "Sem título"
                    val position = LatLng(lat, lng)
                    val marker = googleMap.addMarker(MarkerOptions().position(position).title(title))
                    marker?.let {
                        poiMarkers[it] = document.data
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao carregar POIs públicos: ${e.message}", e)
            }
    }

    private fun searchPOIs(query: String) {
        db.collection("POIs")
            .whereEqualTo("publico", true)
            .get()
            .addOnSuccessListener { result ->
                googleMap.clear()
                poiMarkers.clear()
                for (document in result) {
                    val title = document.getString("titulo") ?: "Sem título"
                    if (title.contains(query, ignoreCase = true)) {
                        val lat = document.getDouble("latitude") ?: continue
                        val lng = document.getDouble("longitude") ?: continue
                        val position = LatLng(lat, lng)
                        val marker = googleMap.addMarker(MarkerOptions().position(position).title(title))
                        marker?.let {
                            poiMarkers[it] = document.data
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao pesquisar POIs: ${e.message}", e)
            }
    }

    private fun showPOIDetailsPopup(marker: Marker) {
        val poiData = poiMarkers[marker]
        if (poiData == null) {
            Toast.makeText(this, "POI não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val title = poiData["titulo"] as? String ?: "Sem título"
        val description = poiData["descricao"] as? String ?: "Sem descrição"
        val latitude = poiData["latitude"] as? Double ?: 0.0
        val longitude = poiData["longitude"] as? Double ?: 0.0
        val poi = POI(
            id = poiData["id"] as? String ?: "",
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            distance = 0f,
            imageBase64 = poiData["imagemBase64"] as? String
        )

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_poi_details)
        dialog.setCancelable(true)

        val titleTextView = dialog.findViewById<TextView>(R.id.poi_title)
        val descriptionTextView = dialog.findViewById<TextView>(R.id.poi_description)
        val distanceTextView = dialog.findViewById<TextView>(R.id.poi_distance)
        val closeButton = dialog.findViewById<Button>(R.id.close_button)
        val detailsButton = dialog.findViewById<Button>(R.id.details_button)

        titleTextView.text = poi.title
        descriptionTextView.text = poi.description
        distanceTextView.text = String.format("%.2f km", poi.distance)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        detailsButton.setOnClickListener {
            openPOIDetailFragment(poi)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openPOIDetailFragment(poi: POI) {
        val fragment = POIDetailFragment.newInstance(poi)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }

    override fun onBackPressed() {
        // Handle back navigation
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
            // Update SearchView visibility based on the current fragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.map_container)
            searchView.visibility = if (currentFragment is SupportMapFragment) View.VISIBLE else View.GONE
        }
    }
}