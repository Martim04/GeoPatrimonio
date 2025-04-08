package com.example.projeto

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddEventFragment : Fragment(), OnMapReadyCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var poiList: List<POI>
    private lateinit var spinnerPoi: Spinner
    private lateinit var editTextEventDate: EditText
    private lateinit var selectedPoiId: String
    private lateinit var selectedDate: String
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_event, container, false)

        db = FirebaseFirestore.getInstance()
        spinnerPoi = view.findViewById(R.id.spinnerPoi)
        editTextEventDate = view.findViewById(R.id.editTextEventDate)
        val editTextEventTitle = view.findViewById<EditText>(R.id.editTextEventTitle)
        val editTextEventDescription = view.findViewById<EditText>(R.id.editTextEventDescription)
        val buttonAddEvent = view.findViewById<Button>(R.id.buttonAddEvent)

        loadPois()

        editTextEventDate.setOnClickListener {
            showDatePickerDialog()
        }

        buttonAddEvent.setOnClickListener {
            val eventTitle = editTextEventTitle.text.toString()
            val eventDescription = editTextEventDescription.text.toString()

            if (eventTitle.isNotEmpty() && eventDescription.isNotEmpty() && ::selectedPoiId.isInitialized && ::selectedDate.isInitialized && selectedLocation != null) {
                addEventToFirestore(eventTitle, eventDescription, selectedPoiId, selectedDate, selectedLocation!!)
            } else {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Localização do Evento"))
            selectedLocation = latLng
        }

        val defaultLocation = LatLng(41.15, -8.6)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
    }

    private fun loadPois() {
        db.collection("POIs")
            .whereEqualTo("publico", true)
            .get()
            .addOnSuccessListener { result ->
                val pois = result.map { document ->
                    POI(
                        id = document.id,
                        title = document.getString("titulo") ?: "",
                        description = document.getString("descricao") ?: "",
                        latitude = document.getDouble("latitude") ?: 0.0,
                        longitude = document.getDouble("longitude") ?: 0.0,
                        imageBase64 = document.getString("imagemBase64")
                    )
                }
                updatePoiList(pois)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao carregar POIs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePoiList(pois: List<POI>) {
        val poiTitles = pois.map { it.title }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, poiTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPoi.adapter = adapter

        spinnerPoi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPoiId = pois[position].id
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                editTextEventDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun addEventToFirestore(title: String, description: String, poiId: String, date: String, location: LatLng) {
        val eventData = hashMapOf(
            "titulo" to title,
            "descricao" to description,
            "id_poi" to poiId,
            "data_evento" to date,
            "data_criacao" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            "latitude" to location.latitude,
            "longitude" to location.longitude
        )

        db.collection("Eventos").add(eventData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Evento adicionado com sucesso", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao adicionar evento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}