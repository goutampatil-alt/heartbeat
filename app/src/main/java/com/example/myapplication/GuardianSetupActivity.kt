package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class GuardianSetupActivity : AppCompatActivity() {

    private lateinit var etGuardianEmail: EditText
    private lateinit var cardCurrentGuardian: MaterialCardView
    private lateinit var tvCurrentGuardianEmail: TextView

    companion object {
        private const val TAG = "GuardianSetup"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_setup)

        etGuardianEmail = findViewById(R.id.etGuardianEmail)
        cardCurrentGuardian = findViewById(R.id.cardCurrentGuardian)
        tvCurrentGuardianEmail = findViewById(R.id.tvCurrentGuardianEmail)

        val btnSave = findViewById<MaterialCardView>(R.id.btnSaveGuardian)
        val btnSkip = findViewById<TextView>(R.id.btnSkipGuardian)
        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnRemove = findViewById<TextView>(R.id.btnRemoveGuardian)

        loadCurrentGuardian()

        btnSave.setOnClickListener {
            val email = etGuardianEmail.text.toString().trim()
            if (email.isEmpty() || !email.contains("@")) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveGuardian(email)
        }

        btnSkip.setOnClickListener { goToDashboard() }
        btnBack.setOnClickListener { goToDashboard() }
        btnRemove.setOnClickListener { removeGuardian() }
    }

    private fun loadCurrentGuardian() {
        try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val guardianEmail = doc.getString("guardianEmail")
                    if (!guardianEmail.isNullOrBlank()) {
                        cardCurrentGuardian.visibility = View.VISIBLE
                        tvCurrentGuardianEmail.text = guardianEmail
                        etGuardianEmail.setText(guardianEmail)
                    }
                }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not available")
        }
    }

    private fun saveGuardian(email: String) {
        try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Toast.makeText(this, "Sign in first", Toast.LENGTH_SHORT).show()
                return
            }
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("guardianEmail", email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Guardian saved! ✅", Toast.LENGTH_SHORT).show()
                    goToDashboard()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase not configured", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeGuardian() {
        try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("guardianEmail", "")
                .addOnSuccessListener {
                    cardCurrentGuardian.visibility = View.GONE
                    etGuardianEmail.text.clear()
                    Toast.makeText(this, "Guardian removed", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not available")
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
