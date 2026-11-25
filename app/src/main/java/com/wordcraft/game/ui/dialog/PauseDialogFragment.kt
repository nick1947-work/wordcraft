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
import com.wordcraft.game.R
import com.wordcraft.game.databinding.DialogPauseBinding

class PauseDialogFragment : DialogFragment() {

    interface PauseDialogListener {
        fun onResumeClicked()
        fun onRestartClicked()
        fun onLevelSelectClicked()
        fun onExitClicked()
        fun onToggleSound()
        fun onToggleHaptics()
    }

    var listener: PauseDialogListener? = null
    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    private var _binding: DialogPauseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPauseBinding.inflate(inflater, container, false)
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

        binding.btnResume.setOnClickListener {
            dismiss()
            listener?.onResumeClicked()
        }

        binding.btnRestart.setOnClickListener {
            dismiss()
            listener?.onRestartClicked()
        }

        binding.btnPauseLevelSelect.setOnClickListener {
            dismiss()
            listener?.onLevelSelectClicked()
        }

        binding.btnPauseExit.setOnClickListener {
            dismiss()
            listener?.onExitClicked()
        }

        updateToggles()

        binding.btnToggleSound.setOnClickListener {
            listener?.onToggleSound()
            soundEnabled = !soundEnabled
            updateToggles()
        }

        binding.btnToggleHaptics.setOnClickListener {
            listener?.onToggleHaptics()
            hapticsEnabled = !hapticsEnabled
            updateToggles()
        }
    }

    private fun updateToggles() {
        binding.btnToggleSound.setImageResource(
            if (soundEnabled) R.drawable.ic_volume_up else R.drawable.ic_volume_mute
        )
        binding.btnToggleHaptics.setColorFilter(
            if (hapticsEnabled) Color.parseColor("#FFB703") else Color.parseColor("#888888")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PauseDialog"
    }
}
