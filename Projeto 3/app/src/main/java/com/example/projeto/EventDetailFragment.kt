package com.example.projeto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class EventDetailFragment : Fragment(), OnMapReadyCallback {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var event: Event
    private lateinit var poiTitleTextView: TextView
    private lateinit var mMap: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var transitInstructionsTextView: TextView
    private lateinit var routeOptionsSpinner: Spinner
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var poiLatLng: LatLng? = null
    private var userLocation: LatLng? = null
    private var currentPolyline: Polyline? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var isMapSetup = false
    private var transitRoutes: List<Pair<String, List<LatLng>>> = emptyList()

    companion object {
        private const val ARG_EVENT = "event"
        private const val TAG = "EventDetailFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        fun newInstance(event: Event): EventDetailFragment {
            return EventDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_EVENT, event)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event_detail, container, false)
        event = arguments?.getParcelable(ARG_EVENT) ?: throw IllegalArgumentException("Event is required")

        poiTitleTextView = view.findViewById(R.id.poi_title)
        transitInstructionsTextView = view.findViewById(R.id.transit_instructions)
        routeOptionsSpinner = view.findViewById(R.id.route_options_spinner)
        mapView = view.findViewById(R.id.map_view)
        view.findViewById<TextView>(R.id.event_title).text = event.title
        view.findViewById<TextView>(R.id.event_description).text = event.description
        view.findViewById<TextView>(R.id.event_date).text = event.eventDate

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        loadPoiDetails(event.poiId)
        setupTransportButtons(view)

        return view
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        mapView.onDestroy()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun setupTransportButtons(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_walking)?.setOnClickListener {
            drawRouteWithMode(userLocation, poiLatLng, "walking")
            toggleTransitViews(false)
        }
        view.findViewById<MaterialButton>(R.id.btn_driving)?.setOnClickListener {
            drawRouteWithMode(userLocation, poiLatLng, "driving")
            toggleTransitViews(false)
        }
        view.findViewById<MaterialButton>(R.id.btn_transit)?.setOnClickListener {
            drawTransitRoutes(userLocation, poiLatLng)
        }
    }

    private fun toggleTransitViews(visible: Boolean) {
        transitInstructionsTextView.visibility = if (visible) View.VISIBLE else View.GONE
        routeOptionsSpinner.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun loadPoiDetails(poiId: String) {
        db.collection("POIs").document(poiId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    poiTitleTextView.text = document.getString("titulo") ?: "POI sem título"
                    poiLatLng = LatLng(
                        document.getDouble("latitude") ?: 0.0,
                        document.getDouble("longitude") ?: 0.0
                    )
                    if (::mMap.isInitialized && !isMapSetup) setupMap()
                } else {
                    poiTitleTextView.text = "POI não encontrado"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao carregar POI: ${e.message}", e)
                poiTitleTextView.text = "Erro ao carregar POI"
                Toast.makeText(context, "Falha ao carregar POI", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isZoomGesturesEnabled = true
            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isTiltGesturesEnabled = true
            uiSettings.isRotateGesturesEnabled = true
        }

        if (checkLocationPermission()) {
            setupLocationAndMap()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun setupLocationAndMap() {
        try {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    userLocation = location?.let { LatLng(it.latitude, it.longitude) }
                    if (poiLatLng != null && !isMapSetup) setupMap()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao obter localização: ${e.message}", e)
                    Toast.makeText(context, "Erro ao obter localização", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Erro ao habilitar localização: ${e.message}", e)
            Toast.makeText(context, "Permissão de localização necessária", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMap() {
        poiLatLng?.let { destination ->
            destinationMarker = mMap.addMarker(MarkerOptions().position(destination).title(event.title))
            userLocation?.let { origin ->
                originMarker = mMap.addMarker(MarkerOptions().position(origin).title("Sua Localização"))
                val bounds = LatLngBounds.builder()
                    .include(origin)
                    .include(destination)
                    .build()
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } ?: mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 15f))
            isMapSetup = true
        } ?: Log.w(TAG, "poiLatLng é nulo")
    }

    private fun drawRouteWithMode(origin: LatLng?, destination: LatLng?, mode: String) {
        if (!validateRouteParams(origin, destination)) return

        thread {
            try {
                val url = buildDirectionsUrl(origin!!, destination!!, mode)
                val jsonObject = fetchDirections(url)
                if (jsonObject.getString("status") == "OK") {
                    val path = parseRoutePath(jsonObject)
                    updateMapWithRoute(path, mode, origin, destination)
                } else {
                    handleRouteError(jsonObject.getString("status"))
                }
            } catch (e: Exception) {
                handleRouteException(e)
            }
        }
    }

    private fun drawTransitRoutes(origin: LatLng?, destination: LatLng?) {
        if (!validateRouteParams(origin, destination)) return

        thread {
            try {
                val url = buildDirectionsUrl(origin!!, destination!!, "transit", true)
                val jsonObject = fetchDirections(url)
                if (jsonObject.getString("status") == "OK") {
                    transitRoutes = parseTransitRoutes(jsonObject)
                    updateMapWithTransitRoutes()
                } else {
                    handleRouteError(jsonObject.getString("status"))
                }
            } catch (e: Exception) {
                handleRouteException(e)
            }
        }
    }

    private fun validateRouteParams(origin: LatLng?, destination: LatLng?): Boolean {
        if (origin == null || destination == null || !::mMap.isInitialized) {
            activity?.runOnUiThread {
                Toast.makeText(context, "Localização ou mapa não disponível", Toast.LENGTH_SHORT).show()
            }
            return false
        }
        return true
    }

    private fun buildDirectionsUrl(origin: LatLng, destination: LatLng, mode: String, alternatives: Boolean = false): String {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}&" +
                "destination=${destination.latitude},${destination.longitude}&" +
                "mode=$mode" +
                (if (alternatives) "&alternatives=true" else "") +
                "&key=${resources.getString(R.string.google_maps_key)}"
    }

    private fun fetchDirections(url: String): JSONObject {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000 // 10 segundos
            connection.readTimeout = 10000
            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(jsonText)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar direções: ${e.message}", e)
            JSONObject().put("status", "REQUEST_FAILED")
        }
    }

    private fun parseRoutePath(jsonObject: JSONObject): List<LatLng> {
        val routes = jsonObject.getJSONArray("routes")
        val steps = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        val path = ArrayList<LatLng>()
        for (i in 0 until steps.length()) {
            val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
            path.addAll(PolyUtil.decode(points))
        }
        return path
    }

    private fun parseTransitRoutes(jsonObject: JSONObject): List<Pair<String, List<LatLng>>> {
        val routes = jsonObject.getJSONArray("routes")
        return (0 until routes.length()).map { i ->
            val route = routes.getJSONObject(i)
            val legs = route.getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")
            val path = ArrayList<LatLng>()
            val instructions = StringBuilder()

            for (j in 0 until steps.length()) {
                val step = steps.getJSONObject(j)
                val points = step.getJSONObject("polyline").getString("points")
                path.addAll(PolyUtil.decode(points))

                val instruction = step.getString("html_instructions")
                if (step.has("transit_details")) {
                    val transit = step.getJSONObject("transit_details")
                    val line = transit.getJSONObject("line")
                    val vehicle = line.getJSONObject("vehicle").getString("type")
                    val name = line.getString("short_name")
                    val departure = transit.getJSONObject("departure_stop").getString("name")
                    val arrival = transit.getJSONObject("arrival_stop").getString("name")
                    instructions.append("Pegar $vehicle $name em $departure até $arrival\n")
                } else {
                    instructions.append("$instruction\n")
                }
            }
            val duration = legs.getJSONObject(0).getJSONObject("duration").getString("text")
            Pair("Rota ${i + 1} ($duration)", path)
        }
    }

    private fun updateMapWithRoute(path: List<LatLng>, mode: String, origin: LatLng, destination: LatLng) {
        activity?.runOnUiThread {
            currentPolyline?.remove()
            val simplifiedPath = PolyUtil.simplify(path, 10.0) // Simplifica a polilinha
            currentPolyline = mMap.addPolyline(
                PolylineOptions()
                    .addAll(simplifiedPath)
                    .width(10f)
                    .color(when (mode) {
                        "walking" -> requireContext().getColor(android.R.color.holo_green_dark)
                        "driving" -> requireContext().getColor(android.R.color.holo_blue_dark)
                        else -> requireContext().getColor(android.R.color.holo_blue_dark)
                    })
            )
            updateMapMarkersAndBounds(origin, destination)
        }
    }

    private fun updateMapWithTransitRoutes() {
        activity?.runOnUiThread {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, transitRoutes.map { it.first })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            routeOptionsSpinner.adapter = adapter
            toggleTransitViews(true)

            routeOptionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    displayTransitRoute(position)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            displayTransitRoute(0)
        }
    }

    private fun displayTransitRoute(routeIndex: Int) {
        val (description, path) = transitRoutes[routeIndex]
        currentPolyline?.remove()
        val simplifiedPath = PolyUtil.simplify(path, 10.0) // Simplifica a polilinha
        currentPolyline = mMap.addPolyline(
            PolylineOptions()
                .addAll(simplifiedPath)
                .width(10f)
                .color(requireContext().getColor(android.R.color.holo_orange_dark))
        )
        updateMapMarkersAndBounds(userLocation!!, poiLatLng!!)
        transitInstructionsTextView.text = description
    }

    private fun updateMapMarkersAndBounds(origin: LatLng, destination: LatLng) {
        originMarker?.remove()
        destinationMarker?.remove()
        originMarker = mMap.addMarker(MarkerOptions().position(origin).title("Sua Localização"))
        destinationMarker = mMap.addMarker(MarkerOptions().position(destination).title(event.title))
        val bounds = LatLngBounds.builder().include(origin).include(destination).build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun handleRouteError(status: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, "Erro na rota: $status", Toast.LENGTH_SHORT).show()
            toggleTransitViews(false)
        }
        Log.w(TAG, "Status da API Directions: $status")
    }

    private fun handleRouteException(e: Exception) {
        activity?.runOnUiThread {
            Toast.makeText(context, "Erro ao carregar rota", Toast.LENGTH_SHORT).show()
            toggleTransitViews(false)
        }
        Log.e(TAG, "Erro ao traçar rota: ${e.message}", e)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (::mMap.isInitialized) setupLocationAndMap()
            } else {
                Toast.makeText(context, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}