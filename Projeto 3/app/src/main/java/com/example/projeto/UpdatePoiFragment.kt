package com.example.projeto

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class UpdatePoiFragment : Fragment(), OnMapReadyCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var currentUserId: String
    private var selectedImageBase64: String? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var selectedImageView: ImageView
    private lateinit var poiList: MutableList<POI>
    private lateinit var poiAdapter: POIAdapter
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var editTextPoiName: EditText
    private lateinit var editTextPoiDescription: EditText
    private lateinit var buttonBack: Button
    private var selectedPoiId: String? = null // Armazena o ID do POI selecionado

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_update_poi, container, false)

        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        viewFlipper = view.findViewById(R.id.viewFlipper)
        editTextPoiName = view.findViewById(R.id.editTextPoiName)
        editTextPoiDescription = view.findViewById(R.id.editTextPoiDescription)
        val buttonUpdatePoi = view.findViewById<Button>(R.id.buttonUpdatePoi)
        val buttonDeletePoi = view.findViewById<Button>(R.id.buttonDeletePoi)
        val buttonSelectImage = view.findViewById<Button>(R.id.buttonSelectImage)
        buttonBack = view.findViewById(R.id.buttonBack)
        selectedImageView = view.findViewById(R.id.selectedImageView)

        buttonUpdatePoi.setOnClickListener {
            val poiName = editTextPoiName.text.toString()
            val poiDescription = editTextPoiDescription.text.toString()
            val location = selectedLocation // Armazena localmente para evitar smart cast error

            if (poiName.isNotEmpty() && poiDescription.isNotEmpty() && location != null && selectedPoiId != null) {
                updatePoiInFirestore(selectedPoiId!!, poiName, poiDescription, location)
            } else {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        buttonDeletePoi.setOnClickListener {
            if (selectedPoiId != null) {
                deletePoiFromFirestore(selectedPoiId!!)
            } else {
                Toast.makeText(requireContext(), "Erro: Nenhum POI selecionado", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        buttonBack.setOnClickListener {
            viewFlipper.displayedChild = 0
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recyclerView = view.findViewById<RecyclerView>(R.id.poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        poiList = mutableListOf()
        poiAdapter = POIAdapter(poiList) { poi -> openEditView(poi) }
        recyclerView.adapter = poiAdapter

        loadPublicPOIs()

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Localização do POI"))
            selectedLocation = latLng
        }

        val defaultLocation = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
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
            } else {
                Toast.makeText(requireContext(), "Erro ao obter a imagem", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun updatePoiInFirestore(id: String, name: String, description: String, location: LatLng) {
        val poiData = hashMapOf(
            "name" to name,
            "description" to description,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "userId" to currentUserId
        )

        db.collection("POIs").document(id).set(poiData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "POI atualizado com sucesso", Toast.LENGTH_SHORT).show()
                viewFlipper.displayedChild = 0
                loadPublicPOIs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao atualizar POI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deletePoiFromFirestore(id: String) {
        db.collection("POIs").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "POI excluído com sucesso", Toast.LENGTH_SHORT).show()
                viewFlipper.displayedChild = 0
                loadPublicPOIs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao excluir POI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPublicPOIs() {
        db.collection("POIs")
            .whereEqualTo("publico", true)
            .get()
            .addOnSuccessListener { result ->
                poiList.clear()
                for (document in result) {
                    val id = document.id
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lng = document.getDouble("longitude") ?: 0.0
                    val title = document.getString("name") ?: "POI"
                    val description = document.getString("description") ?: ""
                    val poi = POI(id, title, description, lat, lng)
                    poiList.add(poi)
                }
                poiAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao carregar POIs públicos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEditView(poi: POI) {
        viewFlipper.displayedChild = 1
        editTextPoiName.setText(poi.title)
        editTextPoiDescription.setText(poi.description)
        selectedPoiId = poi.id

        selectedLocation = LatLng(poi.latitude, poi.longitude)
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(selectedLocation!!).title("Localização do POI"))
        selectedLocation?.let {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
        }
    }
}