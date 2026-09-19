package com.copdhealthtracker.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.copdhealthtracker.databinding.BottomSheetAddFoodBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddFoodBottomSheet(
    private val onAction: (Action) -> Unit
) : BottomSheetDialogFragment() {

    enum class Action { ADD_FOOD, SCAN_LABEL, SCAN_QR_CODE, PHOTO_LIBRARY }

    private var _binding: BottomSheetAddFoodBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addFoodOption.setOnClickListener { dismissAndEmit(Action.ADD_FOOD) }
        binding.scanLabelOption.setOnClickListener { dismissAndEmit(Action.SCAN_LABEL) }
        binding.scanQrCodeOption.setOnClickListener { dismissAndEmit(Action.SCAN_QR_CODE) }
        binding.photoLibraryOption.setOnClickListener { dismissAndEmit(Action.PHOTO_LIBRARY) }
    }

    private fun dismissAndEmit(action: Action) {
        dismiss()
        onAction(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
