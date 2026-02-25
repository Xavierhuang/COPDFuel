package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.copdhealthtracker.data.model.Medication
import com.copdhealthtracker.databinding.DialogAddMedicationBinding

class AddMedicationDialog(
    private val onSave: (Medication) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddMedicationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddMedicationBinding.inflate(layoutInflater)

        val types = arrayOf("Daily", "Exacerbation")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.medicationTypeSpinner.adapter = adapter

        return AlertDialog.Builder(requireContext())
            .setTitle("Add Medication")
            .setView(binding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (validateAndSave()) dismiss()
                    }
                }
            }
    }

    private fun validateAndSave(): Boolean {
        val name = binding.medicationNameEdit.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), "Enter medication name", Toast.LENGTH_SHORT).show()
            return false
        }
        val dosage = binding.medicationDosageEdit.text.toString().trim()
        val frequency = binding.medicationFrequencyEdit.text.toString().trim()
        val typeIndex = binding.medicationTypeSpinner.selectedItemPosition
        val type = if (typeIndex == 0) "daily" else "exacerbation"

        val medication = Medication(
            name = name,
            dosage = dosage,
            frequency = frequency,
            type = type
        )
        onSave(medication)
        Toast.makeText(requireContext(), "Medication saved", Toast.LENGTH_SHORT).show()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
