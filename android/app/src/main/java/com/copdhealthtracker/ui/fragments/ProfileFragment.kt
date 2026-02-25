package com.copdhealthtracker.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.copdhealthtracker.HipaaAuthorizationActivity
import com.copdhealthtracker.R
import com.copdhealthtracker.databinding.FragmentProfileBinding
import com.copdhealthtracker.utils.AppApplication
import com.copdhealthtracker.utils.HipaaConsentStorage
import com.copdhealthtracker.utils.ReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences
    private lateinit var reportGenerator: ReportGenerator
    private lateinit var hipaaStorage: HipaaConsentStorage

    private val hipaaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) updateHipaaStatus()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        hipaaStorage = HipaaConsentStorage(requireContext())
        val repository = (requireActivity().application as AppApplication).repository
        reportGenerator = ReportGenerator(requireContext(), repository)
        setupViews()
        loadProfileData()
        setupShareButton()
        setupHipaaAuthorization()
        setupLinkDoctor()
        setupSignOut()
        setupPrivacyPolicy()
        updateHipaaStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::hipaaStorage.isInitialized) updateHipaaStatus()
    }

    private fun setupHipaaAuthorization() {
        binding.profileHipaaItem.setOnClickListener {
            startActivity(Intent(requireContext(), HipaaAuthorizationActivity::class.java))
        }
    }

    private fun updateHipaaStatus() {
        binding.profileHipaaValue.text = when {
            hipaaStorage.hasValidConsent() -> getString(
                R.string.hipaa_status_signed,
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(hipaaStorage.getConsentDate()))
            )
            hipaaStorage.isRevoked() -> getString(
                R.string.hipaa_status_revoked,
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(hipaaStorage.getRevokedDate()))
            )
            else -> getString(R.string.hipaa_status_not_signed)
        }
    }

    private fun setupPrivacyPolicy() {
        binding.profilePrivacyItem.setOnClickListener {
            val url = com.copdhealthtracker.BuildConfig.PRIVACY_POLICY_URL
            if (url.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(requireContext(), "Privacy policy link will be added when available.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSignOut() {
        binding.btnSignOut.setOnClickListener {
            (requireActivity().application as AppApplication).copdAuth.signOut()
            startActivity(android.content.Intent(requireContext(), com.copdhealthtracker.LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun setupLinkDoctor() {
        binding.btnLinkDoctor.setOnClickListener {
            if (!hipaaStorage.hasValidConsent()) {
                Toast.makeText(requireContext(), R.string.hipaa_consent_required, Toast.LENGTH_LONG).show()
                hipaaLauncher.launch(Intent(requireContext(), HipaaAuthorizationActivity::class.java))
                return@setOnClickListener
            }
            if (!binding.linkDoctorConsent.isChecked) {
                Toast.makeText(requireContext(), "Please agree to share your data with the practice", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val app = requireActivity().application as AppApplication
            app.copdAuth.getIdToken { tokenResult ->
                val token = tokenResult.getOrNull()
                if (token == null) {
                    Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@getIdToken
                }
                binding.btnLinkDoctor.isEnabled = false
                app.apiClient.consent(token, "default", "default") {
                    it.fold(
                        onSuccess = {
                            app.apiClient.linkDoctor(token, null, null, "default") { linkResult ->
                                binding.btnLinkDoctor.isEnabled = true
                                linkResult.fold(
                                    onSuccess = { Toast.makeText(requireContext(), "Linked to doctor", Toast.LENGTH_SHORT).show() },
                                    onFailure = { e -> Toast.makeText(requireContext(), e.message ?: "Link failed", Toast.LENGTH_SHORT).show() }
                                )
                            }
                        },
                        onFailure = { e ->
                            binding.btnLinkDoctor.isEnabled = true
                            Toast.makeText(requireContext(), e.message ?: "Consent failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
    
    private fun setupShareButton() {
        binding.btnShareReport.setOnClickListener {
            showShareOptions()
        }
    }
    
    private fun showShareOptions() {
        val options = arrayOf("Share via Email", "Share via Text Message", "Share via Other Apps")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Share Health Report")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareViaEmail()
                    1 -> shareViaText()
                    2 -> shareViaOther()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun shareViaEmail() {
        lifecycleScope.launch {
            try {
                val report = reportGenerator.generateFullReport()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_SUBJECT, "COPD Health Tracker Report - ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                try {
                    startActivity(Intent.createChooser(intent, "Send Email"))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun shareViaText() {
        lifecycleScope.launch {
            try {
                val report = reportGenerator.generateFullReport()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                    putExtra("sms_body", report)
                }
                try {
                    startActivity(Intent.createChooser(intent, "Send Text Message"))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No messaging app found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun shareViaOther() {
        lifecycleScope.launch {
            try {
                val report = reportGenerator.generateFullReport()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "COPD Health Tracker Report - ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                startActivity(Intent.createChooser(intent, "Share Report"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupViews() {
        binding.profileTitle.text = "My Profile"
        
        binding.profileAgeItem.setOnClickListener { editField("age", "Age", binding.profileAgeValue.text.toString(), InputType.TYPE_CLASS_NUMBER) }
        binding.profileSexItem.setOnClickListener { editField("sex", "Sex", binding.profileSexValue.text.toString(), InputType.TYPE_CLASS_TEXT) }
        binding.profileWeightItem.setOnClickListener { editField("weight", "Weight", binding.profileWeightValue.text.toString().replace(" lbs", ""), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL) }
        binding.profileHeightItem.setOnClickListener { showHeightPicker() }
        binding.profileDoctorNameItem.setOnClickListener { editField("doctor_name", "Doctor Name", binding.profileDoctorNameValue.text.toString(), InputType.TYPE_CLASS_TEXT) }
        binding.profileDoctorPhoneItem.setOnClickListener { editField("doctor_phone", "Doctor Phone", binding.profileDoctorPhoneValue.text.toString(), InputType.TYPE_CLASS_PHONE) }
        binding.profileEmergencyNameItem.setOnClickListener { editField("emergency_contact_name", "Emergency Contact Name", binding.profileEmergencyNameValue.text.toString(), InputType.TYPE_CLASS_TEXT) }
        binding.profileEmergencyPhoneItem.setOnClickListener { editField("emergency_contact_phone", "Emergency Contact Phone", binding.profileEmergencyPhoneValue.text.toString(), InputType.TYPE_CLASS_PHONE) }
        
        // Health conditions checkboxes - save on change
        binding.checkboxHighCholesterol.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("condition_high_cholesterol", isChecked).apply()
            updateLastUpdated()
        }
        binding.checkboxPulmonaryHypertension.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("condition_pulmonary_hypertension", isChecked).apply()
            updateLastUpdated()
        }
        binding.checkboxNone.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("condition_none", isChecked).apply()
            if (isChecked) {
                // Uncheck others when "None" is selected
                binding.checkboxKidneyDisease.isChecked = false
                binding.checkboxHighCholesterol.isChecked = false
                binding.checkboxPulmonaryHypertension.isChecked = false
                binding.checkboxDialysis.isChecked = false
                binding.checkboxOther.isChecked = false
            }
            updateLastUpdated()
        }
        
        binding.checkboxOther.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("condition_other", isChecked).apply()
            binding.editOtherCondition.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.editOtherCondition.setText("")
                prefs.edit().putString("condition_other_text", "").apply()
            }
            if (isChecked) {
                binding.checkboxNone.isChecked = false
            }
            updateLastUpdated()
        }
        
        binding.editOtherCondition.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                prefs.edit().putString("condition_other_text", binding.editOtherCondition.text.toString()).apply()
                updateLastUpdated()
            }
        }
        
        // Activity level radio buttons
        binding.activityLevelGroup.setOnCheckedChangeListener { _, checkedId ->
            val activityLevel = when (checkedId) {
                binding.radioLowActivity.id -> "low"
                binding.radioModerateActivity.id -> "moderate"
                binding.radioPulmonaryRehab.id -> "pulmonary_rehab"
                binding.radioHighActivity.id -> "high"
                binding.radioExacerbation.id -> "exacerbation"
                binding.radioKidneyDisease.id -> "kidney_disease"
                binding.radioDialysis.id -> "dialysis"
                else -> ""
            }
            prefs.edit().putString("activity_level", activityLevel).apply()
            updateProteinTarget()
            updateLastUpdated()
        }
        
        // Supplemental Oxygen radio buttons
        binding.radioOxygen.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioOxygenYes.id -> {
                    binding.lpmContainer.visibility = View.VISIBLE
                    prefs.edit().putBoolean("uses_oxygen", true).apply()
                    updateLastUpdated()
                }
                binding.radioOxygenNo.id -> {
                    binding.lpmContainer.visibility = View.GONE
                    prefs.edit().putBoolean("uses_oxygen", false).apply()
                    prefs.edit().putString("oxygen_lpm", "").apply()
                    binding.editLpm.setText("")
                    updateLastUpdated()
                }
            }
        }
        
        // BIPAP radio buttons
        binding.radioBipap.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioBipapYes.id -> {
                    binding.bipapDetailsContainer.visibility = View.VISIBLE
                    prefs.edit().putBoolean("uses_bipap", true).apply()
                    updateLastUpdated()
                }
                binding.radioBipapNo.id -> {
                    binding.bipapDetailsContainer.visibility = View.GONE
                    prefs.edit().putBoolean("uses_bipap", false).apply()
                    prefs.edit().putString("bipap_machine_type", "").apply()
                    prefs.edit().putString("bipap_setting", "").apply()
                    binding.radioMachineType.clearCheck()
                    binding.editBipapSetting.setText("")
                    updateLastUpdated()
                }
            }
        }
        
        // Machine type radio buttons
        binding.radioMachineType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioMachineBipap.id -> {
                    prefs.edit().putString("bipap_machine_type", "BIPAP").apply()
                    updateLastUpdated()
                }
                binding.radioMachineNiv.id -> {
                    prefs.edit().putString("bipap_machine_type", "NIV").apply()
                    updateLastUpdated()
                }
            }
        }
        
        // LPM and BIPAP fields - save on focus lost
        binding.editLpm.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                prefs.edit().putString("oxygen_lpm", binding.editLpm.text.toString()).apply()
                updateLastUpdated()
            }
        }
        binding.editBipapSetting.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                prefs.edit().putString("bipap_setting", binding.editBipapSetting.text.toString()).apply()
                updateLastUpdated()
            }
        }
    }
    
    private fun updateLastUpdated() {
        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_updated", dateStr).apply()
        binding.profileLastUpdatedValue.text = dateStr
    }
    
    private fun updateProteinTarget() {
        // Get weight in lbs and convert to kg
        val weightLbs = prefs.getString("weight", null)?.toDoubleOrNull()
        if (weightLbs == null || weightLbs <= 0) {
            binding.proteinTargetValue.text = "-- g/day (set your weight first)"
            prefs.edit().putFloat("protein_target", 0f).apply()
            return
        }
        
        val weightKg = weightLbs / 2.205  // Convert lbs to kg
        
        // Get activity level (now includes kidney disease and dialysis options)
        val activityLevel = prefs.getString("activity_level", "")
        
        if (activityLevel.isNullOrEmpty()) {
            binding.proteinTargetValue.text = "-- g/day (select your activity level above)"
            prefs.edit().putFloat("protein_target", 0f).apply()
            return
        }
        
        // Calculate protein multiplier based on activity level selection
        val proteinMultiplierLow: Double
        val proteinMultiplierHigh: Double
        val conditionNote: String
        
        when (activityLevel) {
            "low" -> {
                // Low activity: 1.0-1.2 g/kg/day
                proteinMultiplierLow = 1.0
                proteinMultiplierHigh = 1.2
                conditionNote = ""
            }
            "moderate", "pulmonary_rehab" -> {
                // Moderate or pulmonary rehab: 1.2-1.4 g/kg/day
                proteinMultiplierLow = 1.2
                proteinMultiplierHigh = 1.4
                conditionNote = ""
            }
            "high", "exacerbation" -> {
                // High activity or exacerbation: 1.6-1.8 g/kg/day
                proteinMultiplierLow = 1.6
                proteinMultiplierHigh = 1.8
                conditionNote = if (activityLevel == "exacerbation") " (COPD exacerbation)" else ""
            }
            "kidney_disease" -> {
                // Kidney disease: 1.0-1.2 g/kg/day
                proteinMultiplierLow = 1.0
                proteinMultiplierHigh = 1.2
                conditionNote = " (kidney disease)"
            }
            "dialysis" -> {
                // On dialysis: 1.2-1.8 g/kg/day
                proteinMultiplierLow = 1.2
                proteinMultiplierHigh = 1.8
                conditionNote = " (dialysis)"
            }
            else -> {
                // Default: moderate 1.2-1.4 g/kg/day
                proteinMultiplierLow = 1.2
                proteinMultiplierHigh = 1.4
                conditionNote = ""
            }
        }
        
        val proteinLow = (weightKg * proteinMultiplierLow).toInt()
        val proteinHigh = (weightKg * proteinMultiplierHigh).toInt()
        val proteinTarget = (proteinLow + proteinHigh) / 2  // Use midpoint as target
        
        // Save the protein target for use in Tracking
        prefs.edit().putFloat("protein_target", proteinTarget.toFloat()).apply()
        prefs.edit().putInt("protein_target_low", proteinLow).apply()
        prefs.edit().putInt("protein_target_high", proteinHigh).apply()
        
        binding.proteinTargetValue.text = "$proteinLow - $proteinHigh g/day$conditionNote"
    }
    
    private fun showHeightPicker() {
        val ctx = requireContext()
        
        // Parse current height
        val currentHeight = prefs.getString("height", null)
        var currentFeet = 5
        var currentInches = 6
        if (!currentHeight.isNullOrEmpty() && currentHeight != "N/A") {
            val parsed = parseHeightToFeetInches(currentHeight)
            if (parsed != null) {
                currentFeet = parsed.first
                currentInches = parsed.second
            }
        }
        
        // Create custom layout
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 32, 48, 32)
        }
        
        // Feet picker
        val feetPicker = android.widget.NumberPicker(ctx).apply {
            minValue = 3
            maxValue = 8
            value = currentFeet
            wrapSelectorWheel = false
        }
        
        // Feet label
        val feetLabel = android.widget.TextView(ctx).apply {
            text = "'"
            textSize = 24f
            setPadding(8, 0, 24, 0)
        }
        
        // Inches picker
        val inchesPicker = android.widget.NumberPicker(ctx).apply {
            minValue = 0
            maxValue = 11
            value = currentInches
            wrapSelectorWheel = true
        }
        
        // Inches label
        val inchesLabel = android.widget.TextView(ctx).apply {
            text = "\""
            textSize = 24f
            setPadding(8, 0, 0, 0)
        }
        
        layout.addView(feetPicker)
        layout.addView(feetLabel)
        layout.addView(inchesPicker)
        layout.addView(inchesLabel)
        
        AlertDialog.Builder(ctx)
            .setTitle("Edit Height")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val feet = feetPicker.value
                val inches = inchesPicker.value
                val heightStr = "${feet}'${inches}\""
                prefs.edit()
                    .putString("height", heightStr)
                    .putString("last_updated", SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()))
                    .apply()
                loadProfileData()
                Toast.makeText(ctx, "Height updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun parseHeightToFeetInches(s: String): Pair<Int, Int>? {
        val t = s.trim()
        if (t.isEmpty()) return null
        
        // Try to parse formats like 5'6", 5' 6", 5 ft 6 in, etc.
        val numbers = t.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        if (numbers.size >= 2) {
            return Pair(numbers[0], numbers[1])
        } else if (numbers.size == 1) {
            // If only one number, assume it's total inches if > 24, otherwise feet
            return if (numbers[0] > 24) {
                Pair(numbers[0] / 12, numbers[0] % 12)
            } else {
                Pair(numbers[0], 0)
            }
        }
        return null
    }
    
    private fun editField(key: String, title: String, currentValue: String, inputType: Int) {
        val input = EditText(requireContext())
        input.inputType = inputType
        val displayValue = if (currentValue == "N/A" || currentValue == "Not set") "" else currentValue
        input.setText(displayValue)
        input.hint = "Enter $title"
        input.setPadding(32, 32, 32, 32)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit $title")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()
                if (!TextUtils.isEmpty(value)) {
                    prefs.edit()
                        .putString(key, value)
                        .putString("last_updated", SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()))
                        .apply()
                    loadProfileData()
                    Toast.makeText(requireContext(), "$title updated successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun loadProfileData() {
        val age = prefs.getString("age", null)
        binding.profileAgeValue.text = if (age.isNullOrEmpty() || age == "Not set") "N/A" else age
        
        val sex = prefs.getString("sex", null)
        binding.profileSexValue.text = if (sex.isNullOrEmpty() || sex == "Not set") "N/A" else sex
        
        val weight = prefs.getString("weight", null)
        binding.profileWeightValue.text = if (weight.isNullOrEmpty() || weight == "Not set") "N/A" else "$weight lbs"
        
        val height = prefs.getString("height", null)
        binding.profileHeightValue.text = if (height.isNullOrEmpty() || height == "Not set") "N/A" else height
        
        // BMI (computed from weight lbs and height inches)
        val bmi = computeBmi(weight, height)
        binding.profileBmiValue.text = bmi ?: "N/A"
        
        val lastUpdated = prefs.getString("last_updated", null)
        binding.profileLastUpdatedValue.text = if (lastUpdated.isNullOrEmpty()) "N/A" else lastUpdated
        
        val doctorName = prefs.getString("doctor_name", null)
        binding.profileDoctorNameValue.text = if (doctorName.isNullOrEmpty() || doctorName == "Not set") "N/A" else doctorName
        
        val doctorPhone = prefs.getString("doctor_phone", null)
        binding.profileDoctorPhoneValue.text = if (doctorPhone.isNullOrEmpty() || doctorPhone == "Not set") "N/A" else doctorPhone
        
        val emergencyName = prefs.getString("emergency_contact_name", null)
        binding.profileEmergencyNameValue.text = if (emergencyName.isNullOrEmpty() || emergencyName == "Not set") "N/A" else emergencyName
        
        val emergencyPhone = prefs.getString("emergency_contact_phone", null)
        binding.profileEmergencyPhoneValue.text = if (emergencyPhone.isNullOrEmpty() || emergencyPhone == "Not set") "N/A" else emergencyPhone
        
        // Load health conditions checkboxes
        binding.checkboxKidneyDisease.isChecked = prefs.getBoolean("condition_kidney_disease", false)
        binding.checkboxHighCholesterol.isChecked = prefs.getBoolean("condition_high_cholesterol", false)
        binding.checkboxPulmonaryHypertension.isChecked = prefs.getBoolean("condition_pulmonary_hypertension", false)
        binding.checkboxDialysis.isChecked = prefs.getBoolean("condition_dialysis", false)
        binding.checkboxNone.isChecked = prefs.getBoolean("condition_none", false)
        binding.checkboxOther.isChecked = prefs.getBoolean("condition_other", false)
        if (prefs.getBoolean("condition_other", false)) {
            binding.editOtherCondition.visibility = View.VISIBLE
            binding.editOtherCondition.setText(prefs.getString("condition_other_text", ""))
        }
        
        // Load oxygen settings
        val usesOxygen = prefs.getBoolean("uses_oxygen", false)
        if (usesOxygen) {
            binding.radioOxygenYes.isChecked = true
            binding.lpmContainer.visibility = View.VISIBLE
        } else if (prefs.contains("uses_oxygen")) {
            binding.radioOxygenNo.isChecked = true
            binding.lpmContainer.visibility = View.GONE
        }
        binding.editLpm.setText(prefs.getString("oxygen_lpm", ""))
        
        // Load BIPAP settings
        val usesBipap = prefs.getBoolean("uses_bipap", false)
        if (usesBipap) {
            binding.radioBipapYes.isChecked = true
            binding.bipapDetailsContainer.visibility = View.VISIBLE
            
            // Load machine type
            val machineType = prefs.getString("bipap_machine_type", "")
            when (machineType) {
                "BIPAP" -> binding.radioMachineBipap.isChecked = true
                "NIV" -> binding.radioMachineNiv.isChecked = true
            }
        } else if (prefs.contains("uses_bipap")) {
            binding.radioBipapNo.isChecked = true
            binding.bipapDetailsContainer.visibility = View.GONE
        }
        binding.editBipapSetting.setText(prefs.getString("bipap_setting", ""))
        
        // Load activity level
        val activityLevel = prefs.getString("activity_level", "")
        when (activityLevel) {
            "low" -> binding.radioLowActivity.isChecked = true
            "moderate" -> binding.radioModerateActivity.isChecked = true
            "pulmonary_rehab" -> binding.radioPulmonaryRehab.isChecked = true
            "high" -> binding.radioHighActivity.isChecked = true
            "exacerbation" -> binding.radioExacerbation.isChecked = true
            "kidney_disease" -> binding.radioKidneyDisease.isChecked = true
            "dialysis" -> binding.radioDialysis.isChecked = true
        }
        
        // Calculate and display protein target
        updateProteinTarget()
    }
    
    private fun computeBmi(weightStr: String?, heightStr: String?): String? {
        if (weightStr.isNullOrEmpty() || heightStr.isNullOrEmpty() || weightStr == "Not set" || heightStr == "Not set") return null
        val weightLbs = weightStr.toDoubleOrNull() ?: return null
        if (weightLbs <= 0) return null
        val heightInches = parseHeightToInches(heightStr) ?: return null
        if (heightInches <= 0) return null
        val bmi = (weightLbs / (heightInches * heightInches)) * 703
        return String.format(Locale.getDefault(), "%.1f", bmi)
    }
    
    private fun parseHeightToInches(s: String): Double? {
        val t = s.trim()
        if (t.isEmpty()) return null
        val numbers = t.split(Regex("[^0-9.]+")).filter { it.isNotEmpty() }.mapNotNull { it.toDoubleOrNull() }
        if (numbers.size >= 2) return numbers[0] * 12 + numbers[1]
        if (numbers.size == 1) return if (numbers[0] < 25) numbers[0] * 12 else numbers[0]
        return null
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
