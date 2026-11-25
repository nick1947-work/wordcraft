package com.wordcraft.game.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.wordcraft.game.databinding.DialogBonusWordsBinding

class BonusWordsDialogFragment : DialogFragment() {

    var bonusWords: Set<String> = emptySet()
    var totalPossible: Int = 0

    private var _binding: DialogBonusWordsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBonusWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvBonusDiscoveredCount.text = "Discovered: ${bonusWords.size} / $totalPossible"

        if (bonusWords.isNotEmpty()) {
            binding.tvBonusWordsList.text = bonusWords.joinToString(", ")
        } else {
            binding.tvBonusWordsList.text = "No bonus words found yet"
        }

        binding.btnCloseBonus.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BonusWordsDialog"
    }
}
