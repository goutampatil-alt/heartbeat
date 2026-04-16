package com.example.myapplication

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvSpO2: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvConfidence: TextView
    private lateinit var tvAiAnalysis: TextView
    private lateinit var tvEcgStatus: TextView
    private lateinit var tvLiveIndicator: TextView
    private lateinit var ecgView: EcgView
    private lateinit var cardStatus: MaterialCardView

    companion object {
        const val STATUS_SAFE     = "SAFE"
        const val STATUS_LOW      = "LOW RISK"
        const val STATUS_HIGH     = "HIGH RISK"
        const val STATUS_CRITICAL = "CRITICAL"

        const val COLOR_SAFE     = "#00FF99"
        const val COLOR_LOW      = "#FFD600"
        const val COLOR_HIGH     = "#FF8C00"
        const val COLOR_CRITICAL = "#FF3B3B"

        private const val TAG = "HeartGuard"
    }

    private var dangerAlertShowing = false
    private var previousStatus = ""
    private var simulationStep = 0
    private var ecgPhase = 0.0
    private var currentDialog: AlertDialog? = null
    private var isActivityAlive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        isActivityAlive = true

        try {
            tvHeartRate = findViewById(R.id.tvHeartRate)
            tvSpO2 = findViewById(R.id.tvSpO2)
            tvStatus = findViewById(R.id.tvStatus)
            tvConfidence = findViewById(R.id.tvConfidence)
            tvAiAnalysis = findViewById(R.id.tvAiAnalysis)
            tvEcgStatus = findViewById(R.id.tvEcgStatus)
            tvLiveIndicator = findViewById(R.id.tvLiveIndicator)
            ecgView = findViewById(R.id.ecgView)
            cardStatus = findViewById(R.id.cardStatus)

            startLocalSimulation()
            startEcgStream()
            startLiveIndicatorBlink()
        } catch (e: Exception) {
            Log.e(TAG, "Error during init: ${e.message}", e)
        }
    }

    private fun startLocalSimulation() {
        lifecycleScope.launch {
            while (isActive && isActivityAlive) {
                try {
                    val data = generateFakeData()
                    updateUI(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Simulation error: ${e.message}", e)
                }
                delay(6000)
            }
        }
    }

    private fun generateFakeData(): HealthData {
        val phase = simulationStep % 4
        simulationStep++

        return when (phase) {
            0 -> HealthData(
                heart_rate = Random.nextInt(65, 85),
                spo2 = Random.nextInt(97, 100),
                status = STATUS_SAFE,
                confidence = Random.nextDouble(0.92, 0.99)
            )
            1 -> HealthData(
                heart_rate = Random.nextInt(90, 105),
                spo2 = Random.nextInt(94, 97),
                status = STATUS_LOW,
                confidence = Random.nextDouble(0.75, 0.88)
            )
            2 -> HealthData(
                heart_rate = Random.nextInt(110, 135),
                spo2 = Random.nextInt(90, 94),
                status = STATUS_HIGH,
                confidence = Random.nextDouble(0.80, 0.93)
            )
            else -> HealthData(
                heart_rate = Random.nextInt(140, 170),
                spo2 = Random.nextInt(82, 90),
                status = STATUS_CRITICAL,
                confidence = Random.nextDouble(0.88, 0.99)
            )
        }
    }

    private fun startEcgStream() {
        try {
            tvEcgStatus.text = "● Receiving"
            tvEcgStatus.setTextColor(Color.parseColor(COLOR_SAFE))
        } catch (e: Exception) {
            Log.e(TAG, "ECG status init error: ${e.message}", e)
        }

        lifecycleScope.launch {
            while (isActive && isActivityAlive) {
                try {
                    val points = mutableListOf<Float>()
                    for (i in 0 until 5) {
                        points.add(generateEcgPoint())
                        ecgPhase += 0.08
                    }
                    ecgView.addDataPoints(points)
                } catch (e: Exception) {
                    Log.e(TAG, "ECG stream error: ${e.message}", e)
                }
                delay(50)
            }
        }
    }

    private fun generateEcgPoint(): Float {
        val t = ecgPhase % (2 * Math.PI)
        val pWave = 0.15f * sin(3.0 * t).toFloat()
        val qrs = if (t in 1.4..1.7) {
            1.0f * sin(20.0 * (t - 1.4)).toFloat()
        } else { 0f }
        val tWave = if (t in 2.2..3.2) {
            0.25f * sin(Math.PI * (t - 2.2) / 1.0).toFloat()
        } else { 0f }
        val noise = Random.nextFloat() * 0.03f - 0.015f
        return pWave + qrs + tWave + noise
    }

    private fun updateUI(data: HealthData) {
        if (!isActivityAlive) return

        tvHeartRate.text = "${data.heart_rate}"
        tvSpO2.text = "${data.spo2}"
        tvStatus.text = data.status.uppercase()
        tvConfidence.text = "Confidence: ${(data.confidence * 100).toInt()}%"

        val statusColor = try {
            when (data.status) {
                STATUS_SAFE     -> Color.parseColor(COLOR_SAFE)
                STATUS_LOW      -> Color.parseColor(COLOR_LOW)
                STATUS_HIGH     -> Color.parseColor(COLOR_HIGH)
                STATUS_CRITICAL -> Color.parseColor(COLOR_CRITICAL)
                else            -> Color.GRAY
            }
        } catch (e: Exception) {
            Color.GRAY
        }

        tvStatus.setTextColor(statusColor)
        cardStatus.strokeColor = statusColor
        ecgView.setStatusColor(statusColor)

        tvAiAnalysis.text = when (data.status) {
            STATUS_SAFE     -> buildSafeAnalysis(data)
            STATUS_LOW      -> buildLowRiskAnalysis(data)
            STATUS_HIGH     -> buildHighRiskAnalysis(data)
            STATUS_CRITICAL -> buildCriticalAnalysis(data)
            else            -> "Analyzing..."
        }

        if (data.status != STATUS_CRITICAL && currentDialog?.isShowing == true) {
            try {
                currentDialog?.dismiss()
                dangerAlertShowing = false
            } catch (e: Exception) {
                Log.e(TAG, "Dialog dismiss error: ${e.message}", e)
            }
        }

        // Send Firestore alerts when status worsens
        if (data.status != previousStatus && data.status != STATUS_SAFE) {
            try {
                NotificationHelper.sendRiskAlert(
                    riskLevel = data.status,
                    heartRate = data.heart_rate,
                    spo2 = data.spo2,
                    confidence = data.confidence
                )
            } catch (e: Exception) {
                Log.e(TAG, "Alert send error: ${e.message}", e)
            }
        }

        // Save health snapshot
        try {
            NotificationHelper.saveHealthSnapshot(
                heartRate = data.heart_rate,
                spo2 = data.spo2,
                status = data.status,
                confidence = data.confidence
            )
        } catch (e: Exception) {
            Log.e(TAG, "Snapshot save error: ${e.message}", e)
        }

        if (data.status == STATUS_CRITICAL) {
            showCriticalDialog()
        }

        previousStatus = data.status
    }

    private fun buildSafeAnalysis(data: HealthData): String {
        return "✅ All vitals within normal range.\n" +
                "Heart rate ${data.heart_rate} BPM is healthy.\n" +
                "SpO2 at ${data.spo2}% indicates good oxygen saturation.\n" +
                "No cardiac anomalies detected in ECG signal."
    }

    private fun buildLowRiskAnalysis(data: HealthData): String {
        return "🟡 Minor irregularity detected.\n" +
                "Heart rate slightly elevated at ${data.heart_rate} BPM.\n" +
                "SpO2 at ${data.spo2}% — within acceptable limits.\n" +
                "Recommend continued monitoring. No immediate action needed."
    }

    private fun buildHighRiskAnalysis(data: HealthData): String {
        return "🟠 Significant anomaly detected.\n" +
                "Heart rate ${data.heart_rate} BPM is notably elevated.\n" +
                "SpO2 at ${data.spo2}% is below optimal levels.\n" +
                "ECG shows irregular patterns. Medical consultation recommended."
    }

    private fun buildCriticalAnalysis(data: HealthData): String {
        return "🚨 CRITICAL: Dangerous pattern detected!\n" +
                "Heart rate ${data.heart_rate} BPM is dangerously high.\n" +
                "SpO2 at ${data.spo2}% — oxygen levels critically low.\n" +
                "ECG shows severe abnormalities. Seek IMMEDIATE medical help."
    }

    private fun showCriticalDialog() {
        if (dangerAlertShowing) return
        if (previousStatus == STATUS_CRITICAL) return
        if (!isActivityAlive || isFinishing || isDestroyed) return

        dangerAlertShowing = true

        try {
            currentDialog = AlertDialog.Builder(this)
                .setTitle("🚨 CRITICAL — Immediate Attention")
                .setMessage(
                    "Severe cardiac anomaly detected.\n\n" +
                    "Heart rate and SpO2 levels are in the danger zone.\n" +
                    "Seek emergency medical help immediately."
                )
                .setCancelable(false)
                .setPositiveButton("ACKNOWLEDGE") { dialog, _ ->
                    dialog.dismiss()
                    dangerAlertShowing = false
                    currentDialog = null
                }
                .create()

            currentDialog?.show()
        } catch (e: Exception) {
            Log.e(TAG, "Dialog show error: ${e.message}", e)
            dangerAlertShowing = false
            currentDialog = null
        }
    }

    private fun startLiveIndicatorBlink() {
        try {
            ValueAnimator.ofFloat(1f, 0.2f).apply {
                duration = 800
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { tvLiveIndicator.alpha = it.animatedValue as Float }
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "Blink animation error: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        isActivityAlive = false
        try {
            currentDialog?.dismiss()
            currentDialog = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}", e)
        }
        super.onDestroy()
    }
}