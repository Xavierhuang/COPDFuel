package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.R
import com.copdhealthtracker.data.model.Medication
import com.copdhealthtracker.utils.AppApplication
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MedicationsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_medications, null)
        val dailyList = view.findViewById<ViewGroup>(R.id.daily_medications_list)
        val exacerbationList = view.findViewById<ViewGroup>(R.id.exacerbation_medications_list)
        val addButton = view.findViewById<android.widget.Button>(R.id.add_medication_button)

        val repo = (requireActivity().application as AppApplication).repository

        fun showMedList(meds: List<Medication>, container: ViewGroup) {
            container.removeAllViews()
            if (meds.isEmpty()) {
                val tv = TextView(requireContext()).apply {
                    text = "None added. Tap Add Medication below."
                    setPadding(0, 8, 0, 8)
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }
                container.addView(tv)
            } else {
                meds.forEach { med ->
                    val tv = TextView(requireContext()).apply {
                        text = "${med.name} – ${med.dosage} (${med.frequency})"
                        setPadding(0, 4, 0, 4)
                    }
                    container.addView(tv)
                }
            }
        }

        lifecycleScope.launch {
            repo.getMedicationsByType("daily").collectLatest { list ->
                showMedList(list, dailyList)
            }
        }
        lifecycleScope.launch {
            repo.getMedicationsByType("exacerbation").collectLatest { list ->
                showMedList(list, exacerbationList)
            }
        }

        addButton.setOnClickListener {
            AddMedicationDialog { medication ->
                lifecycleScope.launch {
                    repo.insertMedication(medication)
                }
            }.show(parentFragmentManager, "AddMedication")
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Medications")
            .setView(view)
            .setNegativeButton("Close", null)
            .create()
    }
}
