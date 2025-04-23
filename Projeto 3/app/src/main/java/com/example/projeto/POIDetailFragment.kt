package com.example.projeto

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.TravelMode
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.MapStyleOptions

class POIDetailFragment : Fragment(), OnMapReadyCallback {
    private lateinit var btnFavorite: AppCompatImageView
    private var isFavorite = false
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var mMap: GoogleMap
    private lateinit var poi: POI
    private lateinit var geoApiContext: GeoApiContext
    private lateinit var routeSelector: Spinner
    private var routes: List<DirectionsRoute> = emptyList()
    private lateinit var commentsContainer: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var btnSubmitComment: Button
    private val db = FirebaseFirestore.getInstance()
    private lateinit var ratingBar: RatingBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var pendingTravelMode: TravelMode? = null

    companion object {
        private const val ARG_POI = "poi"

        fun newInstance(poi: POI): POIDetailFragment {
            val fragment = POIDetailFragment()
            val args = Bundle().apply {
                putParcelable(ARG_POI, poi)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poi_detail, container, false)
        poi = arguments?.getParcelable(ARG_POI) ?: throw IllegalArgumentException("POI is required")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        ratingBar = view.findViewById(R.id.rating_bar)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        geoApiContext = GeoApiContext.Builder()
            .apiKey(getString(R.string.google_maps_key))
            .build()

        routeSelector = view.findViewById(R.id.route_selector)

        view.findViewById<TextView>(R.id.poi_title).text = poi.title
        view.findViewById<TextView>(R.id.poi_description).text = poi.description

        val imageView = view.findViewById<ImageView>(R.id.poi_image)
        poi.imageBase64?.let {
            val imageBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } ?: imageView.setImageResource(R.drawable.placeholder_image)

        view.findViewById<Button>(R.id.btn_drive).setOnClickListener {
            pendingTravelMode = TravelMode.DRIVING
            checkLocationAndGetRoute()
        }
        view.findViewById<Button>(R.id.btn_walk).setOnClickListener {
            pendingTravelMode = TravelMode.WALKING
            checkLocationAndGetRoute()
        }
        view.findViewById<Button>(R.id.btn_transit).setOnClickListener {
            pendingTravelMode = TravelMode.TRANSIT
            checkLocationAndGetRoute()
        }

        commentsContainer = view.findViewById(R.id.comments_recycler_view)
        commentInput = view.findViewById(R.id.comment_input)
        btnSubmitComment = view.findViewById(R.id.btn_submit_comment)

        btnSubmitComment.setOnClickListener {
            val commentText = commentInput.text.toString()
            val rating = ratingBar.rating
            if (commentText.isNotEmpty()) {
                submitComment(commentText, rating)
            }
        }
        btnFavorite = view.findViewById<AppCompatImageView>(R.id.btn_favorite)
        checkIfFavorite()
        btnFavorite.setOnClickListener { toggleFavorite() }

        loadComments()

        return view
    }

    private fun checkIfFavorite() {
        userId?.let { uid ->
            db.collection("Favoritos")
                .whereEqualTo("id_utilizador", uid)
                .whereEqualTo("id_poi", poi.id)
                .get()
                .addOnSuccessListener { documents ->
                    isFavorite = !documents.isEmpty
                    updateFavoriteIcon()
                }
        }
    }

    private fun removeComments(poiId: String) {
        db.collection("Comentarios")
            .whereEqualTo("id_poi", poiId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("Comentarios").document(document.id).delete()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to remove comments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleFavorite() {
        userId?.let { uid ->
            val favRef = db.collection("Favoritos")

            if (isFavorite) {
                favRef.whereEqualTo("id_utilizador", uid)
                    .whereEqualTo("id_poi", poi.id)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            favRef.document(document.id).delete()
                        }
                        isFavorite = false
                        updateFavoriteIcon()
                        removeComments(poi.id)
                        Toast.makeText(requireContext(), "Removido dos favoritos", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val favorito = hashMapOf(
                    "id_utilizador" to uid,
                    "id_poi" to poi.id,
                    "data_adicionado" to FieldValue.serverTimestamp()
                )
                favRef.add(favorito).addOnSuccessListener {
                    isFavorite = true
                    updateFavoriteIcon()
                    Toast.makeText(requireContext(), "Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        val iconRes = if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border
        btnFavorite.setImageResource(iconRes)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            getDeviceLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        val poiLocation = LatLng(poi.latitude, poi.longitude)
        mMap.addMarker(MarkerOptions().position(poiLocation).title(poi.title))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poiLocation, 15f))

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
            mMap.setMapStyle(MapStyleOptions(style))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao aplicar estilo ao mapa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDeviceLocation(callback: (() -> Unit)? = null) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        mMap.addMarker(MarkerOptions().position(currentLocation!!).title("Sua Localização"))
                        callback?.invoke()
                    } else {
                        Toast.makeText(requireContext(), "Não foi possível obter a localização", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erro ao obter localização: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Permissão de localização necessária", Toast.LENGTH_SHORT).show()
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
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    mMap.isMyLocationEnabled = true
                    getDeviceLocation { pendingTravelMode?.let { getRoute(it) } }
                }
            } else {
                Toast.makeText(requireContext(), "Permissão de localização negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationAndGetRoute() {
        if (currentLocation != null) {
            pendingTravelMode?.let { getRoute(it) }
        } else if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getDeviceLocation { pendingTravelMode?.let { getRoute(it) } }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getRoute(travelMode: TravelMode) {
        if (currentLocation == null) {
            Toast.makeText(requireContext(), "Aguardando localização...", Toast.LENGTH_SHORT).show()
            pendingTravelMode = travelMode
            return
        }

        val poiLocation = LatLng(poi.latitude, poi.longitude)

        DirectionsApi.newRequest(geoApiContext)
            .origin(com.google.maps.model.LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
            .destination(com.google.maps.model.LatLng(poiLocation.latitude, poiLocation.longitude))
            .mode(travelMode)
            .alternatives(true)
            .setCallback(object : com.google.maps.PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult) {
                    routes = result.routes.toList()
                    val routeOptions = routes.mapIndexed { index, route ->
                        "Route ${index + 1}: ${route.legs[0].duration.humanReadable}"
                    }
                    activity?.runOnUiThread {
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, routeOptions)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        routeSelector.adapter = adapter
                        routeSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                displayRoute(routes[position], Color.BLUE)
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                        if (routes.isNotEmpty()) {
                            displayRoute(routes[0], Color.BLUE)
                        }
                        pendingTravelMode = null
                    }
                }

                override fun onFailure(e: Throwable) {
                    e.printStackTrace()
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Falha ao carregar rotas: ${e.message}", Toast.LENGTH_SHORT).show()
                        pendingTravelMode = null
                    }
                }
            })
    }

    private fun displayRoute(route: DirectionsRoute, color: Int) {
        val polylineOptions = com.google.android.gms.maps.model.PolylineOptions().color(color)
        val stepsInfo = StringBuilder()
        for (leg in route.legs) {
            for (step in leg.steps) {
                polylineOptions.addAll(step.polyline.decodePath().map { LatLng(it.lat, it.lng) })
                if (step.travelMode == TravelMode.TRANSIT) {
                    val transitDetails = step.transitDetails
                    stepsInfo.append("<b>Take ${transitDetails.line.shortName}</b> from <b>${transitDetails.departureStop.name}</b> to <b>${transitDetails.arrivalStop.name}</b><br>")
                }
            }
        }
        val duration = route.legs[0].duration.humanReadable
        activity?.runOnUiThread {
            mMap.clear()
            mMap.addPolyline(polylineOptions)
            mMap.addMarker(MarkerOptions().position(currentLocation!!).title("Start"))
            mMap.addMarker(MarkerOptions().position(LatLng(poi.latitude, poi.longitude)).title("End"))
            view?.findViewById<TextView>(R.id.route_duration)?.text = "Estimated travel time: $duration"
            view?.findViewById<TextView>(R.id.route_steps)?.text = stepsInfo.toString()
        }
    }

    private fun submitComment(commentText: String, rating: Float) {
        val comment = hashMapOf(
            "id_utilizador" to FirebaseAuth.getInstance().currentUser?.uid,
            "id_poi" to poi.id,
            "comentario" to commentText,
            "avaliacao" to rating,
            "data_comentario" to FieldValue.serverTimestamp()
        )

        db.collection("Comentarios").add(comment).addOnSuccessListener {
            commentInput.text.clear()
            ratingBar.rating = 0f
            loadComments()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to submit comment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadComments() {
        db.collection("Comentarios")
            .whereEqualTo("id_poi", poi.id)
            .orderBy("data_comentario", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val commentList = mutableListOf<Comment>()
                for (document in result) {
                    val commentText = document.getString("comentario") ?: ""
                    val rating = document.getDouble("avaliacao")?.toFloat() ?: 0f
                    commentList.add(Comment(commentText, rating))
                }
                commentsContainer.layoutManager = LinearLayoutManager(context)
                commentsContainer.adapter = CommentAdapter(commentList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load comments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}