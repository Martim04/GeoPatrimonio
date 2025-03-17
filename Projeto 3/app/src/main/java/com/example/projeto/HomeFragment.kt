// HomeFragment.kt
package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private val poiList = mutableListOf<POI>()
    private lateinit var poiAdapter: POIAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        db = FirebaseFirestore.getInstance()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recyclerView = view.findViewById<RecyclerView>(R.id.poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        poiAdapter = POIAdapter(poiList) { poi -> openPoiDetailFragment(poi) }
        recyclerView.adapter = poiAdapter

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        loadAuthorizedPOIs()
    }

    private fun loadAuthorizedPOIs() {
        db.collection("POIs")
            .whereEqualTo("publico", true)
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

    private fun openPoiDetailFragment(poi: POI) {
        val fragment = POIDetailFragment.newInstance(poi)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}