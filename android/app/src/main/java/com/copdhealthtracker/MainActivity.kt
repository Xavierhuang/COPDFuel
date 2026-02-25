package com.copdhealthtracker

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.databinding.ActivityMainBinding
import com.copdhealthtracker.utils.AppApplication
import com.copdhealthtracker.ui.fragments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentTab: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            setActiveTab(0)
        }

        setupBottomNavigation()
        registerAndSyncIfLoggedIn()
    }

    private fun registerAndSyncIfLoggedIn() {
        val app = application as AppApplication
        lifecycleScope.launch {
            val token = suspendCancellableCoroutine<String?> { cont ->
                app.copdAuth.getIdToken { r -> cont.resume(r.getOrNull()) }
            } ?: return@launch
            withContext(Dispatchers.IO) {
                val weights = app.repository.getAllWeights().first()
                val medications = app.repository.getAllMedications().first()
                val oxygen = app.repository.getAllReadings().first()
                val exercises = app.repository.getAllExercises().first()
                val water = app.repository.getAllWaterEntries().first()
                val foods = app.repository.getFoodsByDateRange(0L, System.currentTimeMillis() + 86400000L).first()
                app.apiClient.putMe(token, "") { }
                app.apiClient.sync(token, weights, medications, oxygen, exercises, water, foods) { }
            }
        }
    }
    
    private fun setupBottomNavigation() {
        binding.navHome.setOnClickListener { switchTab(0) }
        binding.navGuidelines.setOnClickListener { switchTab(1) }
        binding.navTracking.setOnClickListener { switchTab(2) }
        binding.navRecipes.setOnClickListener { switchTab(3) }
        binding.navResources.setOnClickListener { switchTab(4) }
        binding.navProfile.setOnClickListener { switchTab(5) }
    }
    
    fun switchToGuidelines() {
        switchTab(1)
    }
    
    fun switchToTracking() {
        switchTab(2)
    }
    
    fun switchToProfile() {
        switchTab(5)
    }
    
    private fun switchTab(tabIndex: Int) {
        if (currentTab == tabIndex) return
        
        val fragment = when (tabIndex) {
            0 -> HomeFragment()
            1 -> GuidelinesFragment()
            2 -> TrackingFragment()
            3 -> RecipesFragment()
            4 -> ResourcesFragment()
            5 -> ProfileFragment()
            else -> HomeFragment()
        }
        
        replaceFragment(fragment)
        setActiveTab(tabIndex)
        currentTab = tabIndex
    }
    
    private fun setActiveTab(tabIndex: Int) {
        // Reset all tabs
        setTabInactive(binding.navHome, binding.navHomeLabel)
        setTabInactive(binding.navGuidelines, binding.navGuidelinesLabel)
        setTabInactive(binding.navTracking, binding.navTrackingLabel)
        setTabInactive(binding.navRecipes, binding.navRecipesLabel)
        setTabInactive(binding.navResources, binding.navResourcesLabel)
        setTabInactive(binding.navProfile, binding.navProfileLabel)
        
        // Set active tab
        when (tabIndex) {
            0 -> setTabActive(binding.navHome, binding.navHomeLabel)
            1 -> setTabActive(binding.navGuidelines, binding.navGuidelinesLabel)
            2 -> setTabActive(binding.navTracking, binding.navTrackingLabel)
            3 -> setTabActive(binding.navRecipes, binding.navRecipesLabel)
            4 -> setTabActive(binding.navResources, binding.navResourcesLabel)
            5 -> setTabActive(binding.navProfile, binding.navProfileLabel)
        }
    }
    
    private fun setTabActive(tabView: View, labelView: TextView) {
        tabView.alpha = 1.0f
        labelView.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        labelView.textSize = 12f
    }
    
    private fun setTabInactive(tabView: View, labelView: TextView) {
        tabView.alpha = 0.6f
        labelView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        labelView.textSize = 11f
    }
    
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
