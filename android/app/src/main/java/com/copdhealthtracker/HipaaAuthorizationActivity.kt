package com.copdhealthtracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.copdhealthtracker.databinding.ActivityHipaaAuthorizationBinding
import com.copdhealthtracker.utils.HipaaConsentStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HipaaAuthorizationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHipaaAuthorizationBinding
    private lateinit var hipaaStorage: HipaaConsentStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHipaaAuthorizationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        hipaaStorage = HipaaConsentStorage(this)

        binding.hipaaDateValue.text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())

        if (hipaaStorage.hasValidConsent()) {
            showSignedState()
        } else if (hipaaStorage.isRevoked()) {
            showRevokedState()
        } else {
            showFormState()
        }

        binding.hipaaSubmit.setOnClickListener { submitConsent() }
        binding.hipaaRevoke.setOnClickListener { confirmRevoke() }
        binding.hipaaDob.setOnClickListener { showDobPicker() }
        binding.healthInfoExpiresDate.setOnClickListener { showExpiryDatePicker() }
        binding.hipaaSignatureClear.setOnClickListener { binding.hipaaSignaturePad.clear() }
        binding.healthInfoExpiresGroup.setOnCheckedChangeListener { _, _ -> updateExpiryDateVisibility() }
        binding.healthInfoExpires1year.isChecked = true
    }

    override fun onSupportNavigateUp(): Boolean {
        setResult(RESULT_CANCELED)
        finish()
        return true
    }

    private fun showFormState() {
        binding.hipaaAgree.visibility = View.VISIBLE
        binding.hipaaPrintedNameLayout.visibility = View.VISIBLE
        binding.hipaaDobLayout.visibility = View.VISIBLE
        binding.hipaaSignatureLayout.visibility = View.VISIBLE
        binding.healthInfoConsentTitle.visibility = View.VISIBLE
        binding.healthInfoConsentBody.visibility = View.VISIBLE
        binding.healthInfoExpiresLabel.visibility = View.VISIBLE
        binding.healthInfoExpiresGroup.visibility = View.VISIBLE
        binding.healthInfoConfirmText.visibility = View.VISIBLE
        updateExpiryDateVisibility()
        binding.hipaaDateLabel.visibility = View.VISIBLE
        binding.hipaaDateValue.visibility = View.VISIBLE
        binding.hipaaSubmit.visibility = View.VISIBLE
        binding.hipaaRevoke.visibility = View.GONE
        binding.hipaaFullText.visibility = View.VISIBLE
    }

    private fun showSignedState() {
        binding.hipaaFullText.visibility = View.GONE
        binding.hipaaAgree.visibility = View.GONE
        binding.hipaaPrintedNameLayout.visibility = View.GONE
        binding.hipaaDobLayout.visibility = View.GONE
        binding.hipaaSignatureLayout.visibility = View.GONE
        binding.healthInfoConsentTitle.visibility = View.GONE
        binding.healthInfoConsentBody.visibility = View.GONE
        binding.healthInfoExpiresLabel.visibility = View.GONE
        binding.healthInfoExpiresGroup.visibility = View.GONE
        binding.healthInfoConfirmText.visibility = View.GONE
        binding.hipaaDateLabel.visibility = View.VISIBLE
        binding.hipaaDateValue.visibility = View.VISIBLE
        binding.hipaaDateValue.text = getString(
            R.string.hipaa_status_signed,
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(hipaaStorage.getConsentDate()))
        )
        binding.hipaaSubmit.visibility = View.GONE
        binding.hipaaRevoke.visibility = View.VISIBLE
    }

    private fun showRevokedState() {
        binding.hipaaFullText.visibility = View.VISIBLE
        binding.hipaaAgree.visibility = View.VISIBLE
        binding.hipaaPrintedNameLayout.visibility = View.VISIBLE
        binding.hipaaDobLayout.visibility = View.VISIBLE
        binding.hipaaSignatureLayout.visibility = View.VISIBLE
        binding.healthInfoConsentTitle.visibility = View.VISIBLE
        binding.healthInfoConsentBody.visibility = View.VISIBLE
        binding.healthInfoExpiresLabel.visibility = View.VISIBLE
        binding.healthInfoExpiresGroup.visibility = View.VISIBLE
        binding.healthInfoConfirmText.visibility = View.VISIBLE
        updateExpiryDateVisibility()
        binding.hipaaDateLabel.visibility = View.VISIBLE
        binding.hipaaDateValue.visibility = View.VISIBLE
        binding.hipaaDateValue.text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        binding.hipaaSubmit.visibility = View.VISIBLE
        binding.hipaaRevoke.visibility = View.GONE
    }

    private fun updateExpiryDateVisibility() {
        val onSelected = binding.healthInfoExpiresOn.isChecked
        binding.healthInfoExpiresDate.visibility = if (onSelected) View.VISIBLE else View.GONE
    }

    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)

    private fun showDobPicker() {
        val cal = Calendar.getInstance()
        val maxDayForMonth = { m: Int, y: Int ->
            val c = Calendar.getInstance()
            c.set(y, m - 1, 1)
            c.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        val maxYear = Calendar.getInstance().get(Calendar.YEAR)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_dob_picker, null)
        val monthPicker = view.findViewById<NumberPicker>(R.id.dob_month)
        val dayPicker = view.findViewById<NumberPicker>(R.id.dob_day)
        val yearEdit = view.findViewById<EditText>(R.id.dob_year)
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.wrapSelectorWheel = false
        dayPicker.minValue = 1
        dayPicker.maxValue = 31
        dayPicker.wrapSelectorWheel = false
        val existingDob = binding.hipaaDob.text?.toString()?.trim()
        if (!existingDob.isNullOrEmpty()) {
            try {
                val parsed = dateFormat.parse(existingDob)
                if (parsed != null) {
                    cal.time = parsed
                    monthPicker.value = cal.get(Calendar.MONTH) + 1
                    dayPicker.value = cal.get(Calendar.DAY_OF_MONTH)
                    yearEdit.setText(cal.get(Calendar.YEAR).toString())
                }
            } catch (_: Exception) { }
        }
        if (yearEdit.text.isNullOrEmpty()) {
            yearEdit.setText(cal.get(Calendar.YEAR).toString())
            monthPicker.value = cal.get(Calendar.MONTH) + 1
            dayPicker.value = cal.get(Calendar.DAY_OF_MONTH)
        }
        val initialYear = yearEdit.text.toString().toIntOrNull()?.coerceIn(1900, maxYear) ?: maxYear
        dayPicker.maxValue = maxDayForMonth(monthPicker.value, initialYear)
        if (dayPicker.value > dayPicker.maxValue) dayPicker.value = dayPicker.maxValue
        monthPicker.setOnValueChangedListener { _, _, newMonth ->
            val y = yearEdit.text.toString().toIntOrNull() ?: maxYear
            dayPicker.maxValue = maxDayForMonth(newMonth, y.coerceIn(1900, maxYear))
            if (dayPicker.value > dayPicker.maxValue) dayPicker.value = dayPicker.maxValue
        }
        yearEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val y = yearEdit.text.toString().toIntOrNull() ?: return@setOnFocusChangeListener
                dayPicker.maxValue = maxDayForMonth(monthPicker.value, y.coerceIn(1900, maxYear))
                if (dayPicker.value > dayPicker.maxValue) dayPicker.value = dayPicker.maxValue
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.health_info_consent_dob_hint)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val yearStr = yearEdit.text.toString().trim()
                val year = yearStr.toIntOrNull()
                if (year == null || year < 1900 || year > maxYear) {
                    Toast.makeText(
                        this,
                        getString(R.string.dob_dialog_invalid, maxYear),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                val month = monthPicker.value
                val maxDay = maxDayForMonth(month, year)
                val day = dayPicker.value.coerceIn(1, maxDay)
                cal.set(year, month - 1, day)
                binding.hipaaDob.setText(dateFormat.format(cal.time))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showExpiryDatePicker() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, 1)
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                binding.healthInfoExpiresDate.setText(dateFormat.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitConsent() {
        binding.hipaaError.visibility = View.GONE
        if (!binding.hipaaAgree.isChecked) {
            binding.hipaaError.text = getString(R.string.hipaa_i_agree)
            binding.hipaaError.visibility = View.VISIBLE
            return
        }
        val printedName = binding.hipaaPrintedName.text?.toString()?.trim().orEmpty()
        if (printedName.isEmpty()) {
            binding.hipaaError.text = getString(R.string.health_info_consent_name_hint)
            binding.hipaaError.visibility = View.VISIBLE
            return
        }
        val dob = binding.hipaaDob.text?.toString()?.trim().orEmpty()
        if (dob.isEmpty()) {
            binding.hipaaError.text = getString(R.string.health_info_consent_dob_required)
            binding.hipaaError.visibility = View.VISIBLE
            return
        }
        if (binding.hipaaSignaturePad.isEmpty()) {
            binding.hipaaError.text = getString(R.string.health_info_consent_signature_required)
            binding.hipaaError.visibility = View.VISIBLE
            return
        }
        val expiryType = when (binding.healthInfoExpiresGroup.checkedRadioButtonId) {
            binding.healthInfoExpires1year.id -> "1_year"
            binding.healthInfoExpiresOn.id -> "on_date"
            binding.healthInfoExpiresUntil.id -> "until_withdrawn"
            else -> "until_withdrawn"
        }
        var expiryDateMillis = 0L
        if (expiryType == "on_date") {
            val expiryStr = binding.healthInfoExpiresDate.text?.toString()?.trim().orEmpty()
            if (expiryStr.isEmpty()) {
                binding.hipaaError.text = getString(R.string.health_info_consent_expiry_date_required)
                binding.hipaaError.visibility = View.VISIBLE
                return
            }
            try {
                expiryDateMillis = dateFormat.parse(expiryStr)?.time ?: 0L
            } catch (_: Exception) {
                binding.hipaaError.text = getString(R.string.health_info_consent_expiry_date_required)
                binding.hipaaError.visibility = View.VISIBLE
                return
            }
        } else if (expiryType == "1_year") {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, 1)
            expiryDateMillis = cal.timeInMillis
        }
        hipaaStorage.saveConsent(printedName, dob, expiryType, expiryDateMillis)
        setResult(RESULT_OK)
        Toast.makeText(this, "Authorization saved.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmRevoke() {
        AlertDialog.Builder(this)
            .setTitle(R.string.hipaa_revoke)
            .setMessage(R.string.hipaa_revoke_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                hipaaStorage.revokeConsent()
                setResult(RESULT_OK)
                Toast.makeText(this, "Authorization revoked.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val REQUEST_HIPAA_CONSENT = 1001
    }
}
