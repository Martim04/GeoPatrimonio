package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class POIMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poi_map, container, false)
        db = FirebaseFirestore.getInstance()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        loadPOIs()
    }

    private fun loadPOIs() {
        db.collection("POIs").get().addOnSuccessListener { result ->
            for (document in result) {
                val lat = document.getDouble("latitude") ?: 0.0
                val lng = document.getDouble("longitude") ?: 0.0
                val title = document.getString("titulo") ?: "POI"
                val position = LatLng(lat, lng)
                mMap.addMarker(MarkerOptions().position(position).title(title))
            }
            if (result.documents.isNotEmpty()) {
                val firstPOI = result.documents[0]
                val lat = firstPOI.getDouble("latitude") ?: 0.0
                val lng = firstPOI.getDouble("longitude") ?: 0.0
                val position = LatLng(lat, lng)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
            }
        }
    }
}