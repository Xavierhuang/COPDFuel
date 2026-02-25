package com.copdhealthtracker.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.copdhealthtracker.R
import com.copdhealthtracker.databinding.FragmentProgramsNearMeBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ProgramsNearMeFragment : Fragment() {

    private var _binding: FragmentProgramsNearMeBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null
    private var filteredPrograms: List<Program> = emptyList()
    private var allPrograms: List<Program> = emptyList()

    // Google Places API Key - In production, this should be stored securely
    private val GOOGLE_PLACES_API_KEY = "[REDACTED_GOOGLE_PLACES_KEY]"

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                updateLocationPermissionUI(true)
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                updateLocationPermissionUI(true)
                getCurrentLocation()
            }
            else -> {
                updateLocationPermissionUI(false)
                showSampleData()
            }
        }
    }

    data class Program(
        val id: String,
        val name: String,
        val address: String,
        val city: String,
        val state: String,
        val zipCode: String,
        val phone: String,
        val distance: String,
        val rating: Float,
        val specialties: List<String>,
        val hours: String,
        val website: String? = null,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    )

    private val samplePrograms = listOf(
        Program(
            id = "1",
            name = "City General Hospital - Pulmonary Rehab",
            address = "1234 Medical Center Dr",
            city = "San Francisco",
            state = "CA",
            zipCode = "94102",
            phone = "(415) 555-0123",
            distance = "2.3 miles",
            rating = 4.8f,
            specialties = listOf("COPD Management", "Exercise Training", "Nutrition Counseling"),
            hours = "Mon-Fri: 8AM-5PM",
            website = "www.citygeneral.org"
        ),
        Program(
            id = "2",
            name = "Bay Area Respiratory Center",
            address = "5678 Health Plaza",
            city = "San Francisco",
            state = "CA",
            zipCode = "94105",
            phone = "(415) 555-0456",
            distance = "3.7 miles",
            rating = 4.6f,
            specialties = listOf("Pulmonary Rehabilitation", "Breathing Techniques", "Lifestyle Coaching"),
            hours = "Mon-Thu: 7AM-6PM, Fri: 7AM-4PM",
            website = "www.bayrespiratory.com"
        ),
        Program(
            id = "3",
            name = "Golden Gate Pulmonary Institute",
            address = "9012 Wellness Way",
            city = "San Francisco",
            state = "CA",
            zipCode = "94110",
            phone = "(415) 555-0789",
            distance = "5.1 miles",
            rating = 4.9f,
            specialties = listOf("Advanced COPD Care", "Exercise Physiology", "Mental Health Support"),
            hours = "Mon-Fri: 6AM-7PM, Sat: 8AM-2PM",
            website = "www.goldengatepulmonary.org"
        ),
        Program(
            id = "4",
            name = "Community Health Pulmonary Program",
            address = "3456 Community Blvd",
            city = "Oakland",
            state = "CA",
            zipCode = "94601",
            phone = "(510) 555-0234",
            distance = "8.2 miles",
            rating = 4.4f,
            specialties = listOf("Community Outreach", "Group Therapy", "Family Education"),
            hours = "Mon-Fri: 9AM-5PM",
            website = "www.communityhealth.org"
        ),
        Program(
            id = "5",
            name = "Stanford Pulmonary Rehabilitation",
            address = "7890 University Ave",
            city = "Palo Alto",
            state = "CA",
            zipCode = "94301",
            phone = "(650) 555-0567",
            distance = "12.5 miles",
            rating = 4.9f,
            specialties = listOf("Research-Based Care", "Advanced Technology", "Multidisciplinary Team"),
            hours = "Mon-Fri: 7AM-6PM",
            website = "www.stanfordhealthcare.org"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramsNearMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupBackButton()
        setupLocationOptions()
        setupSearchInput()
        setupSearchLocationButton()
        setupShowSampleDataButton()

        // Check and request location permissions
        checkLocationPermission()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupLocationOptions() {
        binding.currentLocationOption.setOnClickListener {
            if (hasLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun setupSearchInput() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPrograms(s?.toString() ?: "")
            }
        })
    }

    private fun setupSearchLocationButton() {
        binding.searchLocationButton.setOnClickListener {
            val location = binding.manualLocationInput.text.toString().trim()
            if (location.isNotEmpty()) {
                searchByAddress(location)
            } else {
                Toast.makeText(context, "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupShowSampleDataButton() {
        binding.showSampleDataButton.setOnClickListener {
            showSampleData()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun checkLocationPermission() {
        if (hasLocationPermission()) {
            updateLocationPermissionUI(true)
            getCurrentLocation()
        } else {
            updateLocationPermissionUI(false)
            requestLocationPermission()
        }
    }

    private fun updateLocationPermissionUI(hasPermission: Boolean) {
        if (hasPermission) {
            binding.locationPermissionText.text = "Location permission granted"
            binding.statusIcon.setImageResource(R.drawable.ic_location)
        } else {
            binding.locationPermissionText.text = "Location permission denied. Use manual search."
            binding.statusIcon.setImageResource(R.drawable.ic_location)
        }
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            showSampleData()
            return
        }

        showLoading(true)
        binding.locationStatusText.text = "Getting your location..."
        
        // Hide address check when using current location
        binding.addressCheck.visibility = View.GONE
        binding.addressStatusText.text = "Enter any city, state, or address"

        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    binding.locationStatusText.text = "Location found"
                    binding.locationCheck.visibility = View.VISIBLE
                    searchNearbyPrograms(location.latitude, location.longitude)
                } else {
                    binding.locationStatusText.text = "Could not get location"
                    showSampleData()
                }
            }.addOnFailureListener {
                binding.locationStatusText.text = "Error getting location"
                showSampleData()
            }
        } catch (e: SecurityException) {
            showSampleData()
        }
    }

    private fun searchByAddress(address: String) {
        showLoading(true)
        binding.addressStatusText.text = "Searching..."
        binding.locationCheck.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=${
                    java.net.URLEncoder.encode(address, "UTF-8")
                }&key=$GOOGLE_PLACES_API_KEY"

                val response = withContext(Dispatchers.IO) {
                    URL(geocodeUrl).readText()
                }
                val json = JSONObject(response)
                val status = json.optString("status", "")

                when (status) {
                    "OK" -> {
                        val results = json.getJSONArray("results")
                        if (results.length() > 0) {
                            val location = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")
                            val lat = location.getDouble("lat")
                            val lng = location.getDouble("lng")
                            val formattedAddress = results.getJSONObject(0).optString("formatted_address", address)
                            binding.addressStatusText.text = formattedAddress
                            binding.addressCheck.visibility = View.VISIBLE
                            binding.locationStatusText.text = "Tap to get your current location"
                            searchNearbyPrograms(lat, lng)
                        } else {
                            showLoading(false)
                            binding.addressStatusText.text = "Enter any city, state, or address"
                            binding.addressCheck.visibility = View.GONE
                            Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                            showSampleData()
                        }
                    }
                    "REQUEST_DENIED", "OVER_QUERY_LIMIT", "INVALID_REQUEST" -> {
                        val errorMsg = json.optString("error_message", "")
                        showLoading(false)
                        binding.addressStatusText.text = "Enter any city, state, or address"
                        binding.addressCheck.visibility = View.GONE
                        val userMsg = if (status == "REQUEST_DENIED" && errorMsg.contains("not authorized")) {
                            "Address search not available. Enable Geocoding API for your API key in Google Cloud Console."
                        } else {
                            "Could not search: ${if (errorMsg.isNotEmpty()) errorMsg else status}"
                        }
                        Toast.makeText(context, userMsg, Toast.LENGTH_LONG).show()
                        showSampleData()
                    }
                    else -> {
                        showLoading(false)
                        binding.addressStatusText.text = "Enter any city, state, or address"
                        binding.addressCheck.visibility = View.GONE
                        Toast.makeText(context, "Could not find location", Toast.LENGTH_SHORT).show()
                        showSampleData()
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                binding.addressStatusText.text = "Enter any city, state, or address"
                binding.addressCheck.visibility = View.GONE
                Toast.makeText(context, "Error: ${e.message ?: "search failed"}", Toast.LENGTH_SHORT).show()
                showSampleData()
            }
        }
    }

    private fun searchNearbyPrograms(lat: Double, lng: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val programs = withContext(Dispatchers.IO) {
                    val url = "https://places.googleapis.com/v1/places:searchNearby"
                    val requestBody = """
                    {
                        "includedTypes": ["hospital"],
                        "maxResultCount": 20,
                        "locationRestriction": {
                            "circle": {
                                "center": {"latitude": $lat, "longitude": $lng},
                                "radius": 50000
                            }
                        }
                    }
                """.trimIndent()
                    val connection = URL("$url?key=$GOOGLE_PLACES_API_KEY").openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.nationalPhoneNumber,places.regularOpeningHours")
                    connection.doOutput = true
                    connection.outputStream.use { os -> os.write(requestBody.toByteArray()) }
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val list = mutableListOf<Program>()
                    if (json.has("places")) {
                        val places = json.getJSONArray("places")
                        for (i in 0 until places.length()) {
                            val place = places.getJSONObject(i)
                            val location = place.getJSONObject("location")
                            val placeLat = location.getDouble("latitude")
                            val placeLng = location.getDouble("longitude")
                            list.add(
                                Program(
                                    id = place.optString("id", "place_$i"),
                                    name = place.optJSONObject("displayName")?.optString("text", "Unknown") ?: "Unknown",
                                    address = extractAddressPart(place.optString("formattedAddress", "")),
                                    city = extractCityFromAddress(place.optString("formattedAddress", "")),
                                    state = extractStateFromAddress(place.optString("formattedAddress", "")),
                                    zipCode = extractZipFromAddress(place.optString("formattedAddress", "")),
                                    phone = place.optString("nationalPhoneNumber", "Phone not available"),
                                    distance = calculateDistance(lat, lng, placeLat, placeLng),
                                    rating = place.optDouble("rating", 0.0).toFloat(),
                                    specialties = listOf("Hospital", "Healthcare"),
                                    hours = if (place.has("regularOpeningHours")) "Hours available" else "Hours not available",
                                    latitude = placeLat,
                                    longitude = placeLng
                                )
                            )
                        }
                    }
                    list
                }
                if (programs.isEmpty()) {
                    showSampleData()
                } else {
                    allPrograms = programs
                    filteredPrograms = programs
                    displayPrograms(programs)
                }
                showLoading(false)
            } catch (e: Exception) {
                showLoading(false)
                showSampleData()
            }
        }
    }

    private fun showSampleData() {
        allPrograms = samplePrograms
        filteredPrograms = samplePrograms
        displayPrograms(samplePrograms)
        showLoading(false)
    }

    private fun filterPrograms(query: String) {
        if (query.isEmpty()) {
            filteredPrograms = allPrograms
        } else {
            filteredPrograms = allPrograms.filter { program ->
                program.name.lowercase().contains(query.lowercase()) ||
                program.city.lowercase().contains(query.lowercase()) ||
                program.specialties.any { it.lowercase().contains(query.lowercase()) }
            }
        }
        displayPrograms(filteredPrograms)
    }

    private fun displayPrograms(programs: List<Program>) {
        binding.programsListContainer.removeAllViews()
        binding.resultsCount.text = "${programs.size} program${if (programs.size != 1) "s" else ""} found"

        if (programs.isEmpty()) {
            binding.noResultsContainer.visibility = View.VISIBLE
            binding.resultsContainer.visibility = View.GONE
        } else {
            binding.noResultsContainer.visibility = View.GONE
            binding.resultsContainer.visibility = View.VISIBLE

            programs.forEach { program ->
                val cardView = layoutInflater.inflate(
                    R.layout.item_program_card,
                    binding.programsListContainer,
                    false
                )
                bindProgramCard(cardView, program)
                binding.programsListContainer.addView(cardView)
            }
        }
    }

    private fun bindProgramCard(cardView: View, program: Program) {
        cardView.findViewById<TextView>(R.id.program_name).text = program.name
        cardView.findViewById<TextView>(R.id.rating_text).text = String.format("%.1f", program.rating)
        cardView.findViewById<TextView>(R.id.distance_text).text = "- ${program.distance}"
        cardView.findViewById<TextView>(R.id.address_text).text = 
            "${program.address}, ${program.city}, ${program.state} ${program.zipCode}"
        cardView.findViewById<TextView>(R.id.phone_text).text = program.phone
        cardView.findViewById<TextView>(R.id.hours_text).text = program.hours

        // Setup stars
        val starsContainer = cardView.findViewById<LinearLayout>(R.id.stars_container)
        starsContainer.removeAllViews()
        val fullStars = program.rating.toInt()
        for (i in 0 until fullStars) {
            val star = ImageView(context).apply {
                setImageResource(R.drawable.ic_star)
                layoutParams = LinearLayout.LayoutParams(
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (2 * resources.displayMetrics.density).toInt()
                }
            }
            starsContainer.addView(star)
        }

        // Setup specialties
        val specialtiesContainer = cardView.findViewById<FlexboxLayout>(R.id.specialties_container)
        specialtiesContainer.removeAllViews()
        program.specialties.forEach { specialty ->
            val tagView = TextView(context).apply {
                text = specialty
                setTextColor(ContextCompat.getColor(requireContext(), R.color.specialtyText))
                setBackgroundResource(R.drawable.specialty_tag_background)
                textSize = 14f
                val paddingH = (12 * resources.displayMetrics.density).toInt()
                val paddingV = (6 * resources.displayMetrics.density).toInt()
                setPadding(paddingH, paddingV, paddingH, paddingV)
                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), (8 * resources.displayMetrics.density).toInt())
                }
            }
            specialtiesContainer.addView(tagView)
        }

        // Setup call button
        cardView.findViewById<Button>(R.id.call_button).setOnClickListener {
            if (program.phone != "Phone not available") {
                showCallDialog(program)
            } else {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup directions button
        cardView.findViewById<Button>(R.id.directions_button).setOnClickListener {
            showDirectionsDialog(program)
        }
    }

    private fun showCallDialog(program: Program) {
        AlertDialog.Builder(requireContext())
            .setTitle("Call Program")
            .setMessage("Would you like to call ${program.name} at ${program.phone}?")
            .setPositiveButton("Call") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${program.phone}")
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDirectionsDialog(program: Program) {
        val address = "${program.address}, ${program.city}, ${program.state} ${program.zipCode}"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Get Directions")
            .setMessage("Would you like to get directions to ${program.name}?")
            .setPositiveButton("Google Maps") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(address)}")
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.programsListContainer.removeAllViews()
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): String {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = earthRadius * c

        return if (distance < 1) {
            "${(distance * 1000).toInt()}m"
        } else {
            String.format("%.1f km", distance)
        }
    }

    private fun extractAddressPart(address: String): String {
        val parts = address.split(",")
        return parts.firstOrNull()?.trim() ?: address
    }

    private fun extractCityFromAddress(address: String): String {
        val parts = address.split(",")
        return if (parts.size >= 2) parts[parts.size - 2].trim() else "Unknown"
    }

    private fun extractStateFromAddress(address: String): String {
        val parts = address.split(",")
        val lastPart = parts.lastOrNull()?.trim() ?: return "Unknown"
        return lastPart.split(" ").firstOrNull() ?: "Unknown"
    }

    private fun extractZipFromAddress(address: String): String {
        val regex = "\\d{5}".toRegex()
        return regex.find(address)?.value ?: "Unknown"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
