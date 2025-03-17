package com.example.projeto

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.TravelMode

class POIDetailFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var poi: POI
    private lateinit var geoApiContext: GeoApiContext
    private lateinit var routeSelector: Spinner
    private var routes: List<DirectionsRoute> = emptyList()

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

        routeSelector = view.findViewById(R.id.route_selector)

        view.findViewById<TextView>(R.id.poi_title).text = poi.title
        view.findViewById<TextView>(R.id.poi_description).text = poi.description

        // Decode and set the image
        val imageView = view.findViewById<ImageView>(R.id.poi_image)
        poi.imageBase64?.let {
            val imageBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } ?: imageView.setImageResource(R.drawable.placeholder_image) // Placeholder image if no image is available

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
    }

    private fun getRoute(travelMode: TravelMode) {
        val currentLocation = LatLng(41.14961, -8.61099) // Replace with actual current location
        val poiLocation = LatLng(poi.latitude, poi.longitude)

        DirectionsApi.newRequest(geoApiContext)
            .origin(com.google.maps.model.LatLng(currentLocation.latitude, currentLocation.longitude))
            .destination(com.google.maps.model.LatLng(poiLocation.latitude, poiLocation.longitude))
            .mode(travelMode)
            .alternatives(true)
            .setCallback(object : com.google.maps.PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult) {
                    routes = result.routes.toList()
                    val routeOptions = routes.mapIndexed { index, route ->
                        "Route ${index + 1}: ${route.legs[0].duration.humanReadable}"
                    }
                    activity?.runOnUiThread {
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, routeOptions)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        routeSelector.adapter = adapter
                        routeSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                displayRoute(routes[position], Color.BLUE)
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                        displayRoute(routes[0], Color.BLUE)
                    }
                }

                override fun onFailure(e: Throwable) {
                    e.printStackTrace()
                }
            })
    }

    private fun displayRoute(route: DirectionsRoute, color: Int) {
        val polylineOptions = com.google.android.gms.maps.model.PolylineOptions().color(color)
        val stepsInfo = StringBuilder()
        for (leg in route.legs) {
            for (step in leg.steps) {
                polylineOptions.addAll(step.polyline.decodePath().map { LatLng(it.lat, it.lng) })
                if (step.travelMode == TravelMode.TRANSIT) {
                    val transitDetails = step.transitDetails
                    stepsInfo.append("<b>Take ${transitDetails.line.shortName}</b> from <b>${transitDetails.departureStop.name}</b> to <b>${transitDetails.arrivalStop.name}</b><br>")
                }
            }
        }
        val duration = route.legs[0].duration.humanReadable
        activity?.runOnUiThread {
            mMap.clear() // Clear existing polylines and markers
            mMap.addPolyline(polylineOptions)
            mMap.addMarker(MarkerOptions().position(LatLng(41.14961, -8.61099)).title("Start"))
            mMap.addMarker(MarkerOptions().position(LatLng(poi.latitude, poi.longitude)).title("End"))
            view?.findViewById<TextView>(R.id.route_duration)?.text = "Estimated travel time: $duration"
            view?.findViewById<TextView>(R.id.route_steps)?.text = stepsInfo.toString()
        }
    }
}