package com.example.projeto

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class POIMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private val poiList = mutableListOf<POI>()
    private lateinit var poiAdapter: POIAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentUserId: String
    private var userLocation: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poi_map, container, false)
        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Inicializa o fragmento do mapa
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configura o RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        poiAdapter = POIAdapter(poiList) { poi -> openMapFragment(poi) }
        recyclerView.adapter = poiAdapter

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Verificação das permissões de localização
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            getDeviceLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        val style = """
        [
            {
                "featureType": "poi.business",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "poi.attraction",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "poi.government",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "poi.medical",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "poi.park",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "poi.place_of_worship",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "poi.school",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            },
            {
                "featureType": "poi.sports_complex",
                "elementType": "labels",
                "stylers": [{"visibility": "off"}]
            }
        ]
        """.trimIndent()

        try {
            googleMap.setMapStyle(MapStyleOptions(style))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao aplicar estilo ao mapa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDeviceLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = LatLng(location.latitude, location.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f))
                        loadUserPOIs() // Carregar POIs após obter a localização
                    } else {
                        val defaultLocation = LatLng(41.1496, -8.61099) // Porto, Portugal
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                        loadUserPOIs() // Carregar sem distância se localização não disponível
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erro ao obter localização: ${e.message}", Toast.LENGTH_SHORT).show()
                    val defaultLocation = LatLng(41.1496, -8.61099)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                    loadUserPOIs()
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
            val defaultLocation = LatLng(41.1496, -8.61099)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
            loadUserPOIs()
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
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap.isMyLocationEnabled = true
                    getDeviceLocation()
                }
            } else {
                Toast.makeText(requireContext(), "Permissão de localização negada", Toast.LENGTH_SHORT).show()
                val defaultLocation = LatLng(41.1496, -8.61099)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                loadUserPOIs()
            }
        }
    }

    private fun loadUserPOIs() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("POIs")
            .whereEqualTo("criado_por", currentUserId)
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

                    // Calcular a distância se a localização do usuário estiver disponível
                    val distance = userLocation?.let {
                        calculateDistance(it, LatLng(latitude, longitude))
                    } ?: 0f // Default para 0 se não houver localização

                    val poi = POI(id, title, description, latitude, longitude, imageBase64 = imageBase64, distance = distance)
                    poiList.add(poi)
                    addMarker(poi)
                }
                poiAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao carregar POIs: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun addMarker(poi: POI) {
        val location = LatLng(poi.latitude, poi.longitude)
        googleMap.addMarker(MarkerOptions().position(location).title(poi.title))
    }

    private fun openMapFragment(poi: POI) {
        val fragment = POIDetailFragment.newInstance(poi)
        parentFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

