package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FavoritePoisFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: POIAdapter
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite_pois, container, false)
        recyclerView = view.findViewById(R.id.favorite_poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = POIAdapter(mutableListOf()) { poi ->
            openPOIDetailFragment(poi)
        }
        recyclerView.adapter = adapter

        if (userId != null) {
            loadFavoritePOIs()
        } else {
            Toast.makeText(requireContext(), "Usuário não autenticado", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun openPOIDetailFragment(poi: POI) {
        val fragment = POIDetailFragment.newInstance(poi)
        parentFragmentManager.beginTransaction()
            .replace(R.id.map_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadFavoritePOIs() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                userId?.let { uid ->
                    // Buscar IDs dos favoritos
                    val favoriteDocs = db.collection("Favoritos")
                        .whereEqualTo("id_utilizador", uid)
                        .get()
                        .await()

                    val favoritePOIs = mutableListOf<POI>()

                    // Obter localização do usuário
                    val userLocation = getUserLocation()

                    // Buscar detalhes de cada POI favorito
                    for (doc in favoriteDocs) {
                        val poiId = doc.getString("id_poi") ?: continue
                        try {
                            val poiDoc = db.collection("POIs").document(poiId).get().await()
                            val title = poiDoc.getString("titulo") ?: "POI"
                            val description = poiDoc.getString("descricao") ?: ""
                            val latitude = poiDoc.getDouble("latitude") ?: 0.0
                            val longitude = poiDoc.getDouble("longitude") ?: 0.0
                            val imageBase64 = poiDoc.getString("imagemBase64")

                            // Calcular distância
                            val distance = if (userLocation != null) {
                                calculateDistance(userLocation, LatLng(latitude, longitude))
                            } else {
                                0f
                            }

                            favoritePOIs.add(
                                POI(
                                    id = poiId,
                                    title = title,
                                    description = description,
                                    latitude = latitude,
                                    longitude = longitude,
                                    distance = distance,
                                    imageBase64 = imageBase64
                                )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Erro ao carregar POI $poiId", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Atualizar UI com os favoritos
                    adapter.updatePOIs(favoritePOIs)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar favoritos: ${e.message ?: "Falha desconhecida"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun getUserLocation(): LatLng? {
        return try {
            val location = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(requireActivity())
                .lastLocation
                .await()
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        return try {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                start.latitude, start.longitude,
                end.latitude, end.longitude,
                results
            )
            // Converter de metros para quilômetros e arredondar para 2 casas decimais
            (results[0] / 1000).coerceAtLeast(0f).let { (it * 100).toInt().toFloat() / 100 }
        } catch (e: Exception) {
            0f // Retorna 0 em caso de erro
        }
    }
}