package com.copdhealthtracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.billing.BillingManager
import com.copdhealthtracker.utils.AppApplication
import kotlinx.coroutines.launch

class PaywallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val app = application as AppApplication
        val subscribeMonthlyBtn = findViewById<Button>(R.id.paywall_subscribe_monthly)
        val subscribeYearlyBtn = findViewById<Button>(R.id.paywall_subscribe_yearly)
        val restoreBtn = findViewById<Button>(R.id.paywall_restore)
        val progress = findViewById<ProgressBar>(R.id.paywall_progress)
        val termsLink = findViewById<TextView>(R.id.paywall_terms_link)
        val privacyLink = findViewById<TextView>(R.id.paywall_privacy_link)

        fun openUrl(url: String) {
            if (url.isBlank()) {
                Toast.makeText(this, "Link will be added when available.", Toast.LENGTH_SHORT).show()
                return
            }
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }.onFailure {
                Toast.makeText(this, "Unable to open link.", Toast.LENGTH_SHORT).show()
            }
        }

        fun onSubscribeClick(productId: String, button: Button) {
            progress.visibility = View.VISIBLE
            subscribeMonthlyBtn.isEnabled = false
            subscribeYearlyBtn.isEnabled = false
            button.isEnabled = false
            app.billingManager.launchSubscribe(this, productId) { msg ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    subscribeMonthlyBtn.isEnabled = true
                    subscribeYearlyBtn.isEnabled = true
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        termsLink.setOnClickListener { openUrl(BuildConfig.TERMS_OF_USE_URL) }
        privacyLink.setOnClickListener { openUrl(BuildConfig.PRIVACY_POLICY_URL) }

        subscribeMonthlyBtn.setOnClickListener {
            onSubscribeClick(BillingManager.PRODUCT_ID_MONTHLY, subscribeMonthlyBtn)
        }
        subscribeYearlyBtn.setOnClickListener {
            onSubscribeClick(BillingManager.PRODUCT_ID_YEARLY, subscribeYearlyBtn)
        }

        restoreBtn.setOnClickListener {
            progress.visibility = View.VISIBLE
            restoreBtn.isEnabled = false
            app.billingManager.restorePurchases { restored ->
                runOnUiThread {
                    progress.visibility = View.GONE
                    restoreBtn.isEnabled = true
                    if (restored) {
                        Toast.makeText(this, "Premium restored.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "No subscription found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            app.billingManager.isPremium.collect { isPremium ->
                if (isPremium) finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
