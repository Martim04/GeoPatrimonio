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

class POIDetailFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var poi: POI

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poi_detail, container, false)
        poi = arguments?.getParcelable(ARG_POI) ?: throw IllegalArgumentException("POI is required")

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val poiLocation = LatLng(poi.latitude, poi.longitude)
        mMap.addMarker(MarkerOptions().position(poiLocation).title(poi.title))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poiLocation, 15f))
    }

    companion object {
        private const val ARG_POI = "poi"

        fun newInstance(poi: POI): POIDetailFragment {
            val fragment = POIDetailFragment()
            val args = Bundle().apply {
                putParcelable(ARG_POI, poi)
            }
            fragment.arguments = args
            return fragment
        }
    }
}