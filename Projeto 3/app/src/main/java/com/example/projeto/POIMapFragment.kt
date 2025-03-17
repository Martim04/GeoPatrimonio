// POIMapFragment.kt
package com.example.projeto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class POIMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private val poiList = mutableListOf<POI>()
    private lateinit var poiAdapter: POIAdapter
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poi_map, container, false)
        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recyclerView = view.findViewById<RecyclerView>(R.id.poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        poiAdapter = POIAdapter(poiList) { poi -> openMapFragment(poi) }
        recyclerView.adapter = poiAdapter

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        loadUserPOIs()
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun loadUserPOIs() {
        db.collection("POIs")
            .whereEqualTo("criado_por", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                poiList.clear()
                googleMap.clear()
                for (document in result) {
                    val id = document.id
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lng = document.getDouble("longitude") ?: 0.0
                    val title = document.getString("titulo") ?: "POI"
                    val description = document.getString("descricao") ?: ""
                    val poi = POI(id, title, description, lat, lng)
                    poiList.add(poi)
                    googleMap.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(title))
                }
                poiAdapter.notifyDataSetChanged()
            }
    }

    private fun openMapFragment(poi: POI) {
        val fragment = POIDetailFragment.newInstance(poi)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}