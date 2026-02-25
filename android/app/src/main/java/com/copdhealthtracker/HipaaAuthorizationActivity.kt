package com.copdhealthtracker

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.copdhealthtracker.databinding.ActivityHipaaAuthorizationBinding
import com.copdhealthtracker.utils.HipaaConsentStorage
import java.text.SimpleDateFormat
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
    }

    override fun onSupportNavigateUp(): Boolean {
        setResult(RESULT_CANCELED)
        finish()
        return true
    }

    private fun showFormState() {
        binding.hipaaAgree.visibility = View.VISIBLE
        binding.hipaaPrintedNameLayout.visibility = View.VISIBLE
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
        binding.hipaaDateLabel.visibility = View.VISIBLE
        binding.hipaaDateValue.visibility = View.VISIBLE
        binding.hipaaDateValue.text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        binding.hipaaSubmit.visibility = View.VISIBLE
        binding.hipaaRevoke.visibility = View.GONE
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
            binding.hipaaError.text = getString(R.string.hipaa_printed_name_hint)
            binding.hipaaError.visibility = View.VISIBLE
            return
        }
        hipaaStorage.saveConsent(printedName)
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
