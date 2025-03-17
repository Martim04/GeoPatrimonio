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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

class AddPoiFragment : Fragment(), OnMapReadyCallback {

    private lateinit var db: FirebaseFirestore
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var currentUserId: String
    private var selectedImageBase64: String? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var selectedImageView: ImageView

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
        val buttonSelectImage = view.findViewById<Button>(R.id.buttonSelectImage)
        selectedImageView = view.findViewById(R.id.selectedImageView)

        buttonAddPoi.setOnClickListener {
            val poiName = editTextPoiName.text.toString()
            val poiDescription = editTextPoiDescription.text.toString()

            if (poiName.isNotEmpty() && poiDescription.isNotEmpty() && selectedLocation != null) {
                addPoiToFirestore(poiName, poiDescription, selectedLocation!!)
            } else {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            selectedImageBase64 = encodeImageToBase64(imageUri!!)
            val imageStream = requireContext().contentResolver.openInputStream(imageUri)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            selectedImageView.setImageBitmap(selectedImage)
            selectedImageView.visibility = View.VISIBLE
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

    private fun addPoiToFirestore(name: String, description: String, location: LatLng) {
        val poi = hashMapOf(
            "titulo" to name,
            "descricao" to description,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "imagemBase64" to selectedImageBase64, // A imagem agora está em Base64
            "criado_por" to currentUserId,
            "data_criacao" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("POIs").add(poi)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "POI adicionado com sucesso", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao adicionar POI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}