package com.example.projeto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoritePoisFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoritePOIAdapter
    private val favoritePOIs = mutableListOf<FavoritePOI>()
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite_pois, container, false)
        recyclerView = view.findViewById(R.id.favorite_poi_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = FavoritePOIAdapter(favoritePOIs)
        recyclerView.adapter = adapter

        loadFavoritePOIs()

        return view
    }

    private fun loadFavoritePOIs() {
        userId?.let { uid ->
            db.collection("Favoritos")
                .whereEqualTo("id_utilizador", uid)
                .get()
                .addOnSuccessListener { documents ->
                    favoritePOIs.clear()
                    for (document in documents) {
                        val poiId = document.getString("id_poi") ?: continue
                        db.collection("POIs").document(poiId).get().addOnSuccessListener { poiDoc ->
                            val title = poiDoc.getString("titulo") ?: "POI"
                            val imageBase64 = poiDoc.getString("imagemBase64")
                            db.collection("Comentarios")
                                .whereEqualTo("id_poi", poiId)
                                .get()
                                .addOnSuccessListener { comments ->
                                    var totalRating = 0f
                                    for (comment in comments) {
                                        totalRating += comment.getDouble("avaliacao")?.toFloat() ?: 0f
                                    }
                                    val averageRating = if (comments.size() > 0) totalRating / comments.size() else 0f
                                    favoritePOIs.add(FavoritePOI(poiId, title, imageBase64, averageRating))
                                    adapter.notifyDataSetChanged()
                                }
                        }
                    }
                }
        }
    }
}