package com.example.projeto

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private val poiList = mutableListOf<POI>()
    private lateinit var poiAdapter: POIAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
    private var userLocation: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Inicializar o mapa
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar o RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        poiAdapter = POIAdapter(poiList) { poi -> openDetailFragment(poi) }
        recyclerView.adapter = poiAdapter

        return view
    }

    private fun addMarker(poi: POI) {
        val location = LatLng(poi.latitude, poi.longitude)
        googleMap.addMarker(MarkerOptions().position(location).title(poi.title))
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Verificar e solicitar permissões de localização
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            getDeviceLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getDeviceLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f))
                        loadPOIs()
                    } else {
                        val defaultLocation = LatLng(41.1496, -8.61099) // Porto, Portugal
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                        loadPOIs()
                    }
                }
                .addOnFailureListener { e ->
                    val defaultLocation = LatLng(41.1496, -8.61099)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                    loadPOIs()
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
            val defaultLocation = LatLng(41.1496, -8.61099)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
            loadPOIs()
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
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap.isMyLocationEnabled = true
                    getDeviceLocation()
                }
            } else {
                val defaultLocation = LatLng(41.1496, -8.61099)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                loadPOIs()
            }
        }
    }

    private fun loadPOIs() {
        db.collection("POIs")
            .whereEqualTo("publico", true)
            .get()
            .addOnSuccessListener { result ->
                poiList.clear()
                for (document in result) {
                    val id = document.id
                    val title = document.getString("titulo") ?: "POI"
                    val description = document.getString("descricao") ?: ""
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    val imageBase64 = document.getString("imagemBase64")

                    val distance = if (userLocation != null) {
                        calculateDistance(userLocation!!, LatLng(latitude, longitude))
                    } else {
                        0f
                    }

                    val poi = POI(
                        id = id,
                        title = title,
                        description = description,
                        latitude = latitude,
                        longitude = longitude,
                        distance = distance,
                        imageBase64 = imageBase64
                    )

                    poiList.add(poi)
                    addMarker(poi)
                }
                poiAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0] / 1000 // Converter de metros para quilômetros
    }

    private fun openDetailFragment(poi: POI) {
        val fragment = POIDetailFragment.newInstance(poi)
        parentFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment) // Alterado para map_container
            .addToBackStack(null)
            .commit()
    }
}
