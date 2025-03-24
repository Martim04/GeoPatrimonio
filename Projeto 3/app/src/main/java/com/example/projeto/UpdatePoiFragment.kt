package com.example.projeto

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import java.io.InputStream
import POI
import android.graphics.Bitmap
import com.google.android.gms.maps.model.MapStyleOptions

class UpdatePoiFragment : Fragment(), OnMapReadyCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var currentUserId: String
    private var selectedImageBase64: String? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var selectedImageView: ImageView
    private lateinit var poiAdapter: POIAdapter
    private val poiList = mutableListOf<POI>()
    private lateinit var viewFlipper: ViewFlipper
    private var currentPoi: POI? = null
    private lateinit var poi: POI


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_update_poi, container, false)

        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recyclerView = view.findViewById<RecyclerView>(R.id.poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        poiAdapter = POIAdapter(poiList) { poi -> openEditView(poi) }
        recyclerView.adapter = poiAdapter

        selectedImageView = view.findViewById(R.id.selectedImageView)
        viewFlipper = view.findViewById(R.id.viewFlipper)

        view.findViewById<Button>(R.id.buttonSelectImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        view.findViewById<Button>(R.id.buttonUpdatePoi).setOnClickListener {
            val editTextPoiName = view.findViewById<EditText>(R.id.editTextPoiName)
            val editTextPoiDescription = view.findViewById<EditText>(R.id.editTextPoiDescription)
            val name = editTextPoiName.text.toString()
            val description = editTextPoiDescription.text.toString()
            selectedLocation?.let { location ->
                currentPoi?.let { poi ->
                    updatePoiInFirestore(poi.id, name, description, location)
                }
            }
        }

        view.findViewById<Button>(R.id.buttonDeletePoi).setOnClickListener {
            currentPoi?.let { poi ->
                deletePoiFromFirestore(poi.id)
            }
        }

        view.findViewById<Button>(R.id.buttonBack).setOnClickListener {
            viewFlipper.displayedChild = 0
        }

        loadPublicPOIs()

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isRotateGesturesEnabled = true
        googleMap.uiSettings.isTiltGesturesEnabled = true

        // Aplica um estilo de mapa para esconder nomes de lojas, restaurantes e monumentos
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
    private fun updateMapWithPoi(poi: POI) {
        if (::mMap.isInitialized) {
            mMap.clear()
            val poiLocation = LatLng(poi.latitude, poi.longitude)
            mMap.addMarker(MarkerOptions().position(poiLocation).title(poi.title))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poiLocation, 15f))
        } else {
            Toast.makeText(requireContext(), "Mapa ainda não carregado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                selectedImageBase64 = encodeImageToBase64(imageUri)
                val imageStream = requireContext().contentResolver.openInputStream(imageUri)
                val selectedImage = BitmapFactory.decodeStream(imageStream)
                selectedImageView.setImageBitmap(selectedImage)
                selectedImageView.visibility = View.VISIBLE
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }



    private fun openEditView(poi: POI) {
        viewFlipper.displayedChild = 1
        currentPoi = poi

        view?.findViewById<EditText>(R.id.editTextPoiName)?.setText(poi.title)
        view?.findViewById<EditText>(R.id.editTextPoiDescription)?.setText(poi.description)
        selectedLocation = LatLng(poi.latitude, poi.longitude)

        updateMapWithPoi(poi) // Atualiza o mapa corretamente

        poi.imageBase64?.let {
            val imageBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            selectedImageView.setImageBitmap(bitmap)
            selectedImageView.visibility = View.VISIBLE
        } ?: run {
            selectedImageView.visibility = View.GONE
        }
    }

    private fun updatePoiInFirestore(id: String, name: String, description: String, location: LatLng) {
        val poiData = hashMapOf(
            "titulo" to name,
            "descricao" to description,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "criado_por" to currentUserId
        )

        selectedImageBase64?.let {
            poiData["imagemBase64"] = it
        }

        db.collection("POIs").document(id).set(poiData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "POI atualizado com sucesso", Toast.LENGTH_SHORT).show()
                viewFlipper.displayedChild = 0
                loadPublicPOIs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao atualizar POI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPublicPOIs() {
        db.collection("POIs").get().addOnSuccessListener { result ->
            poiList.clear()
            for (document in result) {
                val id = document.id
                val title = document.getString("titulo") ?: "POI"
                val description = document.getString("descricao") ?: ""
                val latitude = document.getDouble("latitude") ?: 0.0
                val longitude = document.getDouble("longitude") ?: 0.0
                val imageBase64 = document.getString("imagemBase64")
                val poi = POI(id, title, description, latitude, longitude, imageBase64 = imageBase64)
                poiList.add(poi)
            }
            poiAdapter.notifyDataSetChanged()
        }
    }
    private fun deletePoiFromFirestore(id: String) {
        db.collection("POIs").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "POI excluído com sucesso", Toast.LENGTH_SHORT).show()
                loadPublicPOIs() // Atualiza a lista após a exclusão
                viewFlipper.displayedChild = 0 // Voltar para a lista de POIs
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao excluir POI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
