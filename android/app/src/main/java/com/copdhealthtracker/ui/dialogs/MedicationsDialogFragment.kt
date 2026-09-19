package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.R
import com.copdhealthtracker.data.model.Medication
import com.copdhealthtracker.utils.AppApplication
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_medications, null)
        val dailyList = view.findViewById<ViewGroup>(R.id.daily_medications_list)
        val exacerbationList = view.findViewById<ViewGroup>(R.id.exacerbation_medications_list)
        val discontinuedList = view.findViewById<ViewGroup>(R.id.discontinued_medications_list)
        val discontinuedSection = view.findViewById<View>(R.id.discontinued_section)
        val addButton = view.findViewById<android.widget.Button>(R.id.add_medication_button)

        val repo = (requireActivity().application as AppApplication).repository
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun makeMedRow(med: Medication, onDiscontinue: (() -> Unit)?, onDelete: () -> Unit): View {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
                gravity = Gravity.CENTER_VERTICAL
            }

            val tv = TextView(requireContext()).apply {
                text = "${med.name} – ${med.dosage} (${med.frequency})"
                setPadding(0, 0, 8, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tv)

            val btnSize = (32 * resources.displayMetrics.density).toInt()

            if (onDiscontinue != null) {
                val discontinueBtn = ImageButton(requireContext()).apply {
                    setImageResource(R.drawable.ic_block)
                    setBackgroundResource(android.R.color.transparent)
                    setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                    contentDescription = "Discontinue medication"
                    setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Discontinue Medication")
                            .setMessage("Mark \"${med.name}\" as discontinued? It will be moved to the discontinued list with today's date.")
                            .setPositiveButton("Discontinue") { _, _ -> onDiscontinue() }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                row.addView(discontinueBtn, LinearLayout.LayoutParams(btnSize, btnSize))
            }

            val deleteBtn = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.ic_delete)
                setBackgroundResource(android.R.color.transparent)
                setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                contentDescription = getString(R.string.remove_medication)
                setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Remove Medication")
                        .setMessage("Permanently remove \"${med.name}\" from the list?")
                        .setPositiveButton("Remove") { _, _ -> onDelete() }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            row.addView(deleteBtn, LinearLayout.LayoutParams(btnSize, btnSize))
            return row
        }

        fun makeDiscontinuedRow(med: Medication, onDelete: () -> Unit): View {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
                gravity = Gravity.CENTER_VERTICAL
            }

            val dateStr = med.discontinuedDate?.let { dateFmt.format(Date(it)) } ?: "Unknown date"
            val tv = TextView(requireContext()).apply {
                text = "${med.name} – ${med.dosage} (${med.frequency})\nDiscontinued: $dateStr"
                setPadding(0, 0, 8, 0)
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(tv)

            val btnSize = (32 * resources.displayMetrics.density).toInt()
            val deleteBtn = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.ic_delete)
                setBackgroundResource(android.R.color.transparent)
                setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                contentDescription = getString(R.string.remove_medication)
                setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Remove Medication")
                        .setMessage("Permanently remove \"${med.name}\" from the list?")
                        .setPositiveButton("Remove") { _, _ -> onDelete() }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            row.addView(deleteBtn, LinearLayout.LayoutParams(btnSize, btnSize))
            return row
        }

        fun showMedList(meds: List<Medication>, container: ViewGroup, showDiscontinueAction: Boolean) {
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
                    val discontinueAction: (() -> Unit)? = if (showDiscontinueAction) {
                        {
                            lifecycleScope.launch {
                                repo.updateMedication(
                                    med.copy(
                                        isDiscontinued = 1,
                                        discontinuedDate = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    } else null

                    container.addView(makeMedRow(med, discontinueAction) {
                        lifecycleScope.launch { repo.deleteMedication(med) }
                    })
                }
            }
        }

        fun showDiscontinuedList(meds: List<Medication>, container: ViewGroup) {
            container.removeAllViews()
            meds.forEach { med ->
                container.addView(makeDiscontinuedRow(med) {
                    lifecycleScope.launch { repo.deleteMedication(med) }
                })
            }
        }

        lifecycleScope.launch {
            repo.getMedicationsByType("daily").collectLatest { list ->
                showMedList(list, dailyList, showDiscontinueAction = true)
            }
        }
        lifecycleScope.launch {
            repo.getMedicationsByType("exacerbation").collectLatest { list ->
                showMedList(list, exacerbationList, showDiscontinueAction = true)
            }
        }
        lifecycleScope.launch {
            repo.getDiscontinuedMedications().collectLatest { list ->
                if (list.isEmpty()) {
                    discontinuedSection?.visibility = View.GONE
                } else {
                    discontinuedSection?.visibility = View.VISIBLE
                    showDiscontinuedList(list, discontinuedList)
                }
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
