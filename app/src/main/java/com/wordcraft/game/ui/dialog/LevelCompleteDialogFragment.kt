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
import com.wordcraft.game.databinding.DialogLevelCompleteBinding

class LevelCompleteDialogFragment : DialogFragment() {

    interface LevelCompleteListener {
        fun onNextLevelClicked()
        fun onLevelSelectClicked()
    }

    var listener: LevelCompleteListener? = null
    var levelNumber: Int = 1
    var coinsEarned: Int = 50
    var hasNextLevel: Boolean = true

    private var _binding: DialogLevelCompleteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLevelCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isCancelable = false
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvCompleteTitle.text = "LEVEL $levelNumber COMPLETE!"
        binding.tvRewardCoins.text = "+$coinsEarned Coins"

        if (hasNextLevel) {
            binding.btnNextLevel.visibility = View.VISIBLE
            binding.btnNextLevel.setOnClickListener {
                dismiss()
                listener?.onNextLevelClicked()
            }
        } else {
            binding.btnNextLevel.visibility = View.GONE
        }

        binding.btnCompleteLevelSelect.setOnClickListener {
            dismiss()
            listener?.onLevelSelectClicked()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LevelCompleteDialog"
    }
}
