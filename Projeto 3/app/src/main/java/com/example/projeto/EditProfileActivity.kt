package com.example.projeto

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.mindrot.jbcrypt.BCrypt

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val buttonBack = findViewById<Button>(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish()
        }
        val editTextNewPassword = findViewById<EditText>(R.id.editTextNewPassword)
        val buttonChangePassword = findViewById<Button>(R.id.buttonChangePassword)

        buttonChangePassword.setOnClickListener {
            val newPassword = editTextNewPassword.text.toString()
            if (newPassword.isNotEmpty()) {
                changePassword(newPassword)
            } else {
                Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePassword(newPassword: String) {
        val user = auth.currentUser
        user?.let {
            val userId = it.uid
            val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

            it.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        db.collection("users").document(userId)
                            .update("password", hashedPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("EditProfileActivity", "Error updating password in Firestore", e)
                                Toast.makeText(this, "Failed to update password in Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("EditProfileActivity", "Error updating password", task.exception)
                        Toast.makeText(this, "Failed to update password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}