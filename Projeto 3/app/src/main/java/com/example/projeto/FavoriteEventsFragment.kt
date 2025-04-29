package com.example.projeto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoriteEventsFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private var eventsAdapter: EventAdapter? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: LatLng? = null
    private var recyclerView: RecyclerView? = null

    companion object {
        private const val TAG = "FavoriteEventsFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private val DEFAULT_LOCATION = LatLng(41.14961, -8.61099) // Porto, Portugal
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite_events, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Configurar RecyclerView
        recyclerView = view.findViewById(R.id.events_recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(context)

        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView não encontrado no layout")
            showToast("Erro: RecyclerView não encontrado")
            return view
        }

        // Obter localização e inicializar adapter
        getUserLocation { location ->
            initializeAdapter(location)
            loadFavoriteEvents()
        }

        return view
    }

    private fun getUserLocation(onLocationObtained: (LatLng) -> Unit) {
        if (!checkLocationPermission()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val latLng = location?.let { LatLng(it.latitude, it.longitude) } ?: DEFAULT_LOCATION
                onLocationObtained(latLng)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Falha ao obter localização: ${e.message}", e)
                showToast("Usando localização padrão")
                onLocationObtained(DEFAULT_LOCATION)
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

    private fun initializeAdapter(location: LatLng) {
        userLocation = location
        eventsAdapter = EventAdapter(location) { event ->
            openEventDetailFragment(event)
        }
        recyclerView?.let {
            it.adapter = eventsAdapter
            if (it.adapter == null) {
                Log.e(TAG, "Falha ao configurar o adapter no RecyclerView")
                showToast("Erro ao configurar a lista de eventos")
            }
        } ?: run {
            Log.e(TAG, "RecyclerView não foi inicializado")
            showToast("Erro ao inicializar a lista de eventos")
        }
    }

    private fun loadFavoriteEvents() {
        db.collection("Eventos")
            .get()
            .addOnSuccessListener { result ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val currentDate = Date() // Data atual

                val events = result.mapNotNull { document ->
                    try {
                        val eventDateStr = document.getString("data_evento") ?: return@mapNotNull null
                        val eventDate = dateFormat.parse(eventDateStr) ?: return@mapNotNull null

                        // Verificar se o evento é futuro (incluindo hoje)
                        if (eventDate.before(currentDate) && !isSameDay(eventDate, currentDate)) {
                            return@mapNotNull null // Ignorar eventos passados
                        }

                        Event(
                            id = document.id,
                            title = document.getString("titulo") ?: "",
                            description = document.getString("descricao") ?: "",
                            creationDate = document.getString("data_criacao") ?: "",
                            eventDate = eventDateStr,
                            poiId = document.getString("id_poi") ?: "",
                            latitude = document.getDouble("latitude") ?: 0.0,
                            longitude = document.getDouble("longitude") ?: 0.0
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao parsear evento ${document.id}: ${e.message}", e)
                        null
                    }
                }
                eventsAdapter?.updateEvents(events) ?: Log.e(TAG, "eventsAdapter não inicializado ao carregar eventos")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao carregar eventos: ${e.message}", e)
                showToast("Falha ao carregar eventos")
            }
    }
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
    private fun openEventDetailFragment(event: Event) {
        val fragment = EventDetailFragment.newInstance(event)
        parentFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment) // Alterado para map_container
            .addToBackStack(null)
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation { location ->
                    initializeAdapter(location)
                    loadFavoriteEvents()
                }
            } else {
                showToast("Permissão de localização negada")
                initializeAdapter(DEFAULT_LOCATION)
                loadFavoriteEvents()
            }
        }
    }

    private fun showToast(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        } ?: Log.w(TAG, "Contexto nulo ao tentar exibir Toast: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        eventsAdapter = null
    }
}