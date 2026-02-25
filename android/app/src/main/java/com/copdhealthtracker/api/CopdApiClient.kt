package com.copdhealthtracker.api

import android.os.Handler
import android.os.Looper
import com.copdhealthtracker.BuildConfig
import com.copdhealthtracker.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

/**
 * COPD API client. All methods require a valid JWT (Cognito ID token). Callbacks are invoked on the main thread.
 */
class CopdApiClient {
    private val baseUrl = BuildConfig.API_BASE_URL
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun putMe(token: String, email: String, callback: (Result<Unit>) -> Unit) {
        val body = JSONObject().apply {
            put("email", email)
            put("role", "patient")
        }
        request("PUT", "/me", token, body) { callback(it.map { }) }
    }

    fun sync(
        token: String,
        weights: List<WeightEntry>,
        medications: List<Medication>,
        oxygen: List<OxygenReading>,
        exercises: List<ExerciseEntry>,
        water: List<WaterEntry>,
        foods: List<FoodEntry>,
        callback: (Result<Unit>) -> Unit
    ) {
        val body = JSONObject().apply {
            put("weights", toJsonWeights(weights))
            put("medications", toJsonMedications(medications))
            put("oxygen", toJsonOxygen(oxygen))
            put("exercises", toJsonExercises(exercises))
            put("water", toJsonWater(water))
            put("foods", toJsonFoods(foods))
        }
        request("POST", "/sync", token, body) { callback(it.map { }) }
    }

    fun consent(token: String, practiceId: String?, doctorId: String?, callback: (Result<Unit>) -> Unit) {
        val body = JSONObject().apply {
            put("practiceId", practiceId ?: "")
            put("doctorId", doctorId ?: "")
            put("consentType", "share_with_doctor")
        }
        request("POST", "/consent", token, body) { callback(it.map { }) }
    }

    fun linkDoctor(token: String, inviteCode: String?, doctorId: String?, practiceId: String?, callback: (Result<Unit>) -> Unit) {
        val body = JSONObject().apply {
            inviteCode?.let { put("inviteCode", it) }
            doctorId?.let { put("doctorId", it) }
            practiceId?.let { put("practiceId", it) }
        }
        request("POST", "/link-doctor", token, body) { callback(it.map { }) }
    }

    fun deleteMe(token: String, callback: (Result<Unit>) -> Unit) {
        request("DELETE", "/me", token, null) { callback(it.map { }) }
    }

    private fun request(method: String, path: String, token: String, body: JSONObject?, onResult: (Result<String>) -> Unit) {
        executor.execute {
            try {
                val url = URL("$baseUrl$path")
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = method
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = body != null
                if (method == "DELETE" && body == null) conn.doOutput = false
                conn.doInput = true
                if (body != null) {
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val response = if (code in 200..299) {
                    conn.inputStream?.bufferedReader()?.readText() ?: "{}"
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "{}"
                }
                conn.disconnect()
                if (code in 200..299) mainHandler.post { onResult(Result.success(response)) }
                else {
                    val err = try { JSONObject(response).optString("error", "Error $code") } catch (_: Exception) { "Error $code" }
                    mainHandler.post { onResult(Result.failure(Exception(err))) }
                }
            } catch (e: Exception) {
                mainHandler.post { onResult(Result.failure(e)) }
            }
        }
    }

    private fun toJsonWeights(list: List<WeightEntry>): JSONArray =
        JSONArray().apply { list.forEach { put(JSONObject().apply { put("id", it.id); put("weight", it.weight); put("isGoal", it.isGoal); put("date", it.date) }) } }
    private fun toJsonMedications(list: List<Medication>): JSONArray =
        JSONArray().apply { list.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("dosage", it.dosage); put("frequency", it.frequency); put("type", it.type); put("date", it.date) }) } }
    private fun toJsonOxygen(list: List<OxygenReading>): JSONArray =
        JSONArray().apply { list.forEach { put(JSONObject().apply { put("id", it.id); put("level", it.level); put("date", it.date) }) } }
    private fun toJsonExercises(list: List<ExerciseEntry>): JSONArray =
        JSONArray().apply { list.forEach { put(JSONObject().apply { put("id", it.id); put("type", it.type); put("minutes", it.minutes); put("date", it.date) }) } }
    private fun toJsonWater(list: List<WaterEntry>): JSONArray =
        JSONArray().apply { list.forEach { put(JSONObject().apply { put("id", it.id); put("amount", it.amount); put("date", it.date) }) } }
    private fun toJsonFoods(list: List<FoodEntry>): JSONArray =
        JSONArray().apply { list.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("mealCategory", it.mealCategory); put("quantity", it.quantity); put("calories", it.calories); put("protein", it.protein); put("carbs", it.carbs); put("fat", it.fat); put("date", it.date) }) } }
}
