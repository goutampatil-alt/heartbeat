package com.example.myapplication

import android.util.Log

/**
 * Handles sending alerts to the guardian.
 * Saves alerts to Firestore - Cloud Functions handle email sending.
 * All Firebase calls are safely wrapped — app works fine even without Firebase setup.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"

    fun sendRiskAlert(riskLevel: String, heartRate: Int, spo2: Int, confidence: Double) {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val user = auth.currentUser ?: return

            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    val guardianEmail = doc.getString("guardianEmail") ?: return@addOnSuccessListener

                    // Save alert to Firestore
                    // Cloud Function will automatically pick this up and send email
                    val alert = hashMapOf(
                        "fromUid" to user.uid,
                        "fromEmail" to (user.email ?: ""),
                        "fromName" to (user.displayName ?: "User"),
                        "guardianEmail" to guardianEmail,
                        "heartRate" to heartRate,
                        "spo2" to spo2,
                        "confidence" to confidence,
                        "riskLevel" to riskLevel,
                        "timestamp" to System.currentTimeMillis(),
                        "acknowledged" to false,
                        "emailSent" to false
                    )

                    db.collection("alerts").add(alert)
                        .addOnSuccessListener { docRef ->
                            Log.d(TAG, "Alert saved to Firestore (ID: ${docRef.id})")
                            Log.d(TAG, "Cloud Function will send email to $guardianEmail")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to save alert: ${e.message}", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch user data: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not available, skipping alert")
        }
    }

    fun saveHealthSnapshot(heartRate: Int, spo2: Int, status: String, confidence: Double) {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val user = auth.currentUser ?: return

            val snapshot = hashMapOf(
                "heartRate" to heartRate,
                "spo2" to spo2,
                "status" to status,
                "confidence" to confidence,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("users").document(user.uid)
                .collection("healthData")
                .add(snapshot)
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not available, skipping snapshot")
        }
    }
}
