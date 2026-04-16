package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var progressBar: ProgressBar

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("Login", "Sign-in failed: ${e.statusCode}", e)
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Sign-in failed. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)
        progressBar = findViewById(R.id.progressLogin)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<MaterialCardView>(R.id.btnGoogleSignIn).setOnClickListener {
            progressBar.visibility = View.VISIBLE
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    saveUserData()
                } else {
                    Toast.makeText(this, "Auth failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserData() {
        val user = auth.currentUser ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
            val data = hashMapOf(
                "email" to (user.email ?: ""),
                "name" to (user.displayName ?: ""),
                "fcmToken" to (if (tokenTask.isSuccessful) tokenTask.result else ""),
                "lastLogin" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .set(data)
                .addOnCompleteListener {
                    startActivity(Intent(this, GuardianSetupActivity::class.java))
                    finish()
                }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
