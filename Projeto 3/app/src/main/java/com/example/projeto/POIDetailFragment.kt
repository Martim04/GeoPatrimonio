package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode

class POIDetailFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var poi: POI
    private lateinit var geoApiContext: GeoApiContext
    private val zoomViewModel: ZoomViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poi_detail, container, false)
        poi = arguments?.getParcelable(ARG_POI) ?: throw IllegalArgumentException("POI is required")

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        geoApiContext = GeoApiContext.Builder()
            .apiKey(getString(R.string.google_maps_key))
            .build()

        view.findViewById<Button>(R.id.btn_drive).setOnClickListener { getRoute(TravelMode.DRIVING) }
        view.findViewById<Button>(R.id.btn_walk).setOnClickListener { getRoute(TravelMode.WALKING) }
        view.findViewById<Button>(R.id.btn_transit).setOnClickListener { getRoute(TravelMode.TRANSIT) }


        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val poiLocation = LatLng(poi.latitude, poi.longitude)
        mMap.addMarker(MarkerOptions().position(poiLocation).title(poi.title))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poiLocation, 15f))
        zoomViewModel.setGoogleMap(mMap)
    }

    private fun getRoute(travelMode: TravelMode) {
        val currentLocation = LatLng(41.14961, -8.61099) // Replace with actual current location
        val poiLocation = LatLng(poi.latitude, poi.longitude)

        DirectionsApi.newRequest(geoApiContext)
            .origin(com.google.maps.model.LatLng(currentLocation.latitude, currentLocation.longitude))
            .destination(com.google.maps.model.LatLng(poiLocation.latitude, poiLocation.longitude))
            .mode(travelMode)
            .setCallback(object : com.google.maps.PendingResult.Callback<com.google.maps.model.DirectionsResult> {
                override fun onResult(result: com.google.maps.model.DirectionsResult) {
                    val route = result.routes[0]
                    val polylineOptions = com.google.android.gms.maps.model.PolylineOptions()
                    for (leg in route.legs) {
                        for (step in leg.steps) {
                            polylineOptions.addAll(step.polyline.decodePath().map { LatLng(it.lat, it.lng) })
                        }
                    }
                    val duration = route.legs[0].duration.humanReadable
                    activity?.runOnUiThread {
                        mMap.clear() // Clear existing polylines and markers
                        mMap.addPolyline(polylineOptions)
                        mMap.addMarker(MarkerOptions().position(currentLocation).title("Start"))
                        mMap.addMarker(MarkerOptions().position(poiLocation).title("End"))
                        view?.findViewById<TextView>(R.id.route_duration)?.text = "Estimated travel time: $duration"
                    }
                }

                override fun onFailure(e: Throwable) {
                    e.printStackTrace()
                }
            })
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