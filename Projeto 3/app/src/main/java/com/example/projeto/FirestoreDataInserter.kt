package com.example.projeto

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class FirestoreDataInserter {

    fun insertData(userId: String) {
        val db = FirebaseFirestore.getInstance()

        // Sample POI data
        val poi = hashMapOf(
            "titulo" to "Torre dos Clérigos",
            "descricao" to "Famous bell tower in Porto",
            "latitude" to 41.1455,
            "longitude" to -8.6140,
            "imagem" to "url_to_image",
            "categoria" to "landmark",
            "criado_por" to userId,
            "data_criacao" to FieldValue.serverTimestamp()
        )

        // Add POI to Firestore
        db.collection("POIs").add(poi).addOnSuccessListener { poiDocumentReference ->
            val poiId = poiDocumentReference.id

            // Sample favorite data
            val favorite = hashMapOf(
                "id_utilizador" to userId,
                "id_poi" to poiId,
                "data_adicionado" to FieldValue.serverTimestamp()
            )

            // Add favorite to Firestore
            db.collection("Favoritos").add(favorite).addOnFailureListener { e ->
                Log.e("FirestoreDataInserter", "Error adding favorite", e)
            }

            // Sample notification data
            val notification = hashMapOf(
                "id_utilizador" to userId,
                "titulo" to "Welcome",
                "mensagem" to "Welcome to the app!",
                "lido" to false,
                "data_envio" to FieldValue.serverTimestamp()
            )

            // Add notification to Firestore
            db.collection("Notificacoes").add(notification).addOnFailureListener { e ->
                Log.e("FirestoreDataInserter", "Error adding notification", e)
            }

            // Sample event data
            val event = hashMapOf(
                "titulo" to "Porto Wine Festival",
                "descricao" to "Annual wine festival in Porto",
                "data_evento" to "2024-06-15",
                "localizacao" to "Ribeira, Porto",
                "id_poi" to poiId,
                "data_criacao" to FieldValue.serverTimestamp()
            )

            // Add event to Firestore
            db.collection("Eventos").add(event).addOnFailureListener { e ->
                Log.e("FirestoreDataInserter", "Error adding event", e)
            }

            // Sample visit data
            val visit = hashMapOf(
                "id_utilizador" to userId,
                "id_poi" to poiId,
                "data_visita" to FieldValue.serverTimestamp()
            )

            // Add visit to Firestore
            db.collection("Visitas").add(visit).addOnFailureListener { e ->
                Log.e("FirestoreDataInserter", "Error adding visit", e)
            }

            // Sample preference data
            val preference = hashMapOf(
                "id_utilizador" to userId,
                "notificacoes" to true,
                "categorias_interesse" to "landmark, festival"
            )

            // Add preference to Firestore
            db.collection("Preferencias").add(preference).addOnFailureListener { e ->
                Log.e("FirestoreDataInserter", "Error adding preference", e)
            }

            // Sample comment data
            val comment = hashMapOf(
                "id_utilizador" to userId,
                "id_poi" to poiId,
                "comentario" to "Amazing place!",
                "avaliacao" to 5,
                "data_comentario" to FieldValue.serverTimestamp()
            )

            // Add comment to Firestore
            db.collection("Comentarios").add(comment).addOnFailureListener { e ->
                Log.e("FirestoreDataInserter", "Error adding comment", e)
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreDataInserter", "Error adding POI", e)
        }
    }
}