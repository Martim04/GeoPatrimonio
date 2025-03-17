package com.example.projeto

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminFragment : Fragment() {

    private lateinit var usersAdapter: UsersAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin, container, false)
        db = FirebaseFirestore.getInstance()

        val recyclerView = view.findViewById<RecyclerView>(R.id.users_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        usersAdapter = UsersAdapter(emptyList()) { user -> showRoleDialog(user) }
        recyclerView.adapter = usersAdapter

        loadUsers()

        return view
    }

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val users = result.map { document ->
                    User(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        email = document.getString("email") ?: "",
                        type = document.getString("type") ?: ""
                    )
                }
                usersAdapter.updateUsers(users)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun showRoleDialog(user: User) {
        val roles = arrayOf("admin", "comum")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Role")
        builder.setItems(roles) { _, which ->
            val selectedRole = roles[which]
            updateRole(user, selectedRole)
        }
        builder.show()
    }

    private fun updateRole(user: User, newRole: String) {
        db.collection("users").document(user.id)
            .update("type", newRole)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Role updated successfully", Toast.LENGTH_SHORT).show()
                loadUsers() // Reload users to reflect changes
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}