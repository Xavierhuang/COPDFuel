package com.copdhealthtracker.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.copdhealthtracker.R
import com.copdhealthtracker.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }
    
    private fun setupViews() {
        // Hero section is already set up in the layout
        binding.heroTitle.text = "Breathe Easier with Better Nutrition"
        binding.heroSubtitle.text = "Personalized dietary guidance for managing COPD and improving your quality of life through proper nutrition."
        
        binding.exploreButton.setOnClickListener {
            // Navigate to Guidelines tab
            (activity as? com.copdhealthtracker.MainActivity)?.switchToGuidelines()
        }
        
        binding.sectionTitle.text = "Understanding COPD"
        binding.sectionSubtitle.text = "Learn about Chronic Obstructive Pulmonary Disease and its impact on health."
        
        binding.copdTitle.text = "What is COPD?"
        binding.copdText1.text = "COPD (Chronic Obstructive Pulmonary Disease) is a chronic lung disease that includes conditions such as Emphysema and Bronchiectasis. It causes obstructed airflow from the lungs, making it difficult to breathe."
        binding.copdText2.text = "Approximately 16 million adults have been diagnosed with COPD, and many more may have the disease without a formal diagnosis. COPD is the third leading cause of death globally."
        
        binding.symptomsTitle.text = "Common Symptoms"
        val symptoms = listOf(
            "Shortness of breath, especially during physical activities",
            "Chronic cough that may produce mucus",
            "Wheezing",
            "Chest tightness",
            "Frequent respiratory infections",
            "Lack of energy",
            "Unintended weight loss (in later stages)",
            "Swelling in ankles, feet or legs"
        )
        
        binding.symptomsList.removeAllViews()
        symptoms.forEach { symptom ->
            val symptomView = layoutInflater.inflate(R.layout.item_symptom, binding.symptomsList, false)
            val symptomText = symptomView.findViewById<android.widget.TextView>(R.id.symptom_text)
            symptomText.text = symptom
            binding.symptomsList.addView(symptomView)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
