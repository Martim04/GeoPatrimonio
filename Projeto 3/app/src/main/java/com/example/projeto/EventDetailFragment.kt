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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class EventDetailFragment : Fragment(), OnMapReadyCallback {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var event: Event
    private lateinit var poiTitleTextView: TextView
    private lateinit var mMap: GoogleMap
    private lateinit var transitInstructionsTextView: TextView
    private lateinit var routeOptionsSpinner: Spinner
    private var poiLatLng: LatLng? = null
    private var currentPolyline: com.google.android.gms.maps.model.Polyline? = null
    private var isMapSetup = false
    private val bolhaoLocation = LatLng(41.1496, -8.6075) // Mercado do Bolhão
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event_detail, container, false)
        event = arguments?.getParcelable(ARG_EVENT) ?: throw IllegalArgumentException("Event is required")

        // Inicializar views
        poiTitleTextView = view.findViewById(R.id.poi_title)
        transitInstructionsTextView = view.findViewById(R.id.transit_instructions)
        routeOptionsSpinner = view.findViewById(R.id.route_options_spinner)
        view.findViewById<TextView>(R.id.event_title).text = event.title
        view.findViewById<TextView>(R.id.event_description).text = event.description
        view.findViewById<TextView>(R.id.event_date).text = event.eventDate

        // Carregar detalhes do POI
        loadPoiDetails(event.poiId)

        // Configurar mapa
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this) ?: Log.e(TAG, "Map fragment not found")

        // Configurar botões de transporte
        setupTransportButtons(view)

        return view
    }

    private fun setupTransportButtons(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_walking)?.setOnClickListener {
            drawRouteWithMode(bolhaoLocation, poiLatLng, "walking")
            toggleTransitViews(false)
        }
        view.findViewById<MaterialButton>(R.id.btn_driving)?.setOnClickListener {
            drawRouteWithMode(bolhaoLocation, poiLatLng, "driving")
            toggleTransitViews(false)
        }
        view.findViewById<MaterialButton>(R.id.btn_transit)?.setOnClickListener {
            drawTransitRoutes(bolhaoLocation, poiLatLng)
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
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun setupLocationAndMap() {
        try {
            mMap.isMyLocationEnabled = true
            if (poiLatLng != null && !isMapSetup) setupMap()
        } catch (e: SecurityException) {
            Log.e(TAG, "Erro ao habilitar localização: ${e.message}", e)
            Toast.makeText(context, "Permissão de localização necessária", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMap() {
        poiLatLng?.let { destination ->
            mMap.addMarker(MarkerOptions().position(destination).title(event.title))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 15f))
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
                "&key=${getString(R.string.google_maps_key)}"
    }

    private fun fetchDirections(url: String): JSONObject {
        return JSONObject(URL(url).readText())
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
            currentPolyline = mMap.addPolyline(
                PolylineOptions()
                    .addAll(path)
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
        currentPolyline = mMap.addPolyline(
            PolylineOptions()
                .addAll(path)
                .width(10f)
                .color(requireContext().getColor(android.R.color.holo_orange_dark))
        )
        updateMapMarkersAndBounds(bolhaoLocation, poiLatLng!!)
        transitInstructionsTextView.text = description
    }

    private fun updateMapMarkersAndBounds(origin: LatLng, destination: LatLng) {
        mMap.addMarker(MarkerOptions().position(origin).title("Mercado do Bolhão"))
        mMap.addMarker(MarkerOptions().position(destination).title(event.title))
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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (::mMap.isInitialized) setupLocationAndMap()
        } else {
            Toast.makeText(context, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentPolyline?.remove()
    }
}