package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddPoiFragment : Fragment(), OnMapReadyCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private val zoomViewModel: ZoomViewModel by activityViewModels()
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_poi, container, false)

        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val editTextPoiName = view.findViewById<EditText>(R.id.editTextPoiName)
        val editTextPoiDescription = view.findViewById<EditText>(R.id.editTextPoiDescription)
        val buttonAddPoi = view.findViewById<Button>(R.id.buttonAddPoi)

        buttonAddPoi.setOnClickListener {
            val poiName = editTextPoiName.text.toString()
            val poiDescription = editTextPoiDescription.text.toString()

            if (poiName.isNotEmpty() && poiDescription.isNotEmpty() && selectedLocation != null) {
                addPoiToFirestore(poiName, poiDescription, selectedLocation!!)
            } else {
                Toast.makeText(requireContext(), "Por favor, preencha todos os campos e selecione uma localização", Toast.LENGTH_SHORT).show()
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        zoomViewModel.setGoogleMap(mMap)

        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Localização do POI"))
            selectedLocation = latLng
        }

        // Move the camera to a default location (e.g., user's current location)
        val defaultLocation = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
    }

    private fun addPoiToFirestore(name: String, description: String, location: LatLng) {
        val poi = hashMapOf(
            "titulo" to name,
            "descricao" to description,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "criado_por" to currentUserId,
            "data_criacao" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("POIs")
            .add(poi)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "POI adicionado com sucesso", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e: Exception ->
                Toast.makeText(requireContext(), "Erro ao adicionar POI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}