package com.example.myapplication
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

data class EmailAlertPayload(
    val guardian_email: String,
    val patient_name: String,
    val patient_email: String,
    val risk_level: String,
    val heart_rate: Int,
    val spo2: Int,
    val timestamp: Long
)

interface HealthApiService {
    @GET("latest-data")
    suspend fun getLatestData(): HealthData

    @POST("send-email-alert")
    suspend fun sendEmailAlert(@Body payload: EmailAlertPayload): Map<String, Any>
}