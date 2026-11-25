package com.wordcraft.game.ui.fragment

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.CycleInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wordcraft.game.R
import com.wordcraft.game.databinding.FragmentGameBinding
import com.wordcraft.game.domain.FeedbackType
import com.wordcraft.game.ui.dialog.BonusWordsDialogFragment
import com.wordcraft.game.ui.dialog.LevelCompleteDialogFragment
import com.wordcraft.game.ui.dialog.PauseDialogFragment
import com.wordcraft.game.view.LetterWheelView
import com.wordcraft.game.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    private val viewModel: GameViewModel by activityViewModels()
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private var hasShownCelebration = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle system back navigation to show pause dialog
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showPauseDialog()
                }
            }
        )

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.letterWheelView.listener = object : LetterWheelView.OnWordGestureListener {
            override fun onLetterSelected(index: Int, letter: String) {
                viewModel.onTouchDown(index)
            }

            override fun onWordUpdated(currentWord: String) {
                if (currentWord.isNotEmpty()) {
                    binding.tvWordPreview.visibility = View.VISIBLE
                    binding.tvWordPreview.text = currentWord
                    binding.tvWordPreview.setBackgroundResource(R.drawable.bg_preview_pill)
                    binding.tvWordPreview.setTextColor(Color.parseColor("#0D1B2A"))
                } else {
                    binding.tvWordPreview.visibility = View.INVISIBLE
                }
            }

            override fun onWordCompleted(word: String) {
                viewModel.onTouchUp()
            }
        }

        binding.btnShuffle.setOnClickListener {
            viewModel.shuffleWheel()
            binding.letterWheelView.animateShuffle()
        }

        binding.btnHint.setOnClickListener {
            viewModel.useHint()
        }

        binding.btnPause.setOnClickListener {
            showPauseDialog()
        }

        binding.btnBonusWords.setOnClickListener {
            showBonusWordsDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val level = state.currentLevel ?: return@collect

                    binding.tvGameLevelNumber.text = "LEVEL ${level.id}"
                    binding.tvGameLevelProgress.text =
                        "${level.title} • ${state.solvedWordsCount}/${state.totalWordsCount}"
                    binding.tvGameCoins.text = state.coins.toString()

                    // Hint button state
                    binding.btnHint.alpha = if (state.coins >= 25) 1.0f else 0.5f

                    // Bonus count
                    if (state.bonusWordsFound.isNotEmpty()) {
                        binding.tvBonusWordsCount.visibility = View.VISIBLE
                        binding.tvBonusWordsCount.text = state.bonusWordsFound.size.toString()
                    } else {
                        binding.tvBonusWordsCount.visibility = View.GONE
                    }

                    // Letter wheel
                    binding.letterWheelView.setLetters(state.wheelLetters)

                    // Crossword grid
                    binding.crosswordGridView.setGrid(state.gridMatrix)
                    if (state.recentlySolvedWord != null) {
                        binding.crosswordGridView.animateWordReveal()
                    }

                    // Word Feedback
                    handleFeedback(state.feedbackType, state.feedbackText)

                    // Level completion
                    if (state.isLevelComplete && !hasShownCelebration) {
                        hasShownCelebration = true
                        binding.confettiView.startCelebration()
                        showLevelCompleteDialog(level.id, state.allLevels.size)
                    } else if (!state.isLevelComplete) {
                        hasShownCelebration = false
                    }
                }
            }
        }
    }

    private fun handleFeedback(type: FeedbackType, text: String) {
        if (type == FeedbackType.NONE) {
            if (binding.tvWordPreview.text.isEmpty()) {
                binding.tvWordPreview.visibility = View.INVISIBLE
            }
            return
        }

        binding.tvWordPreview.visibility = View.VISIBLE
        binding.tvWordPreview.text = text

        when (type) {
            FeedbackType.VALID -> {
                binding.tvWordPreview.setBackgroundColor(Color.parseColor("#52B788"))
                binding.tvWordPreview.setTextColor(Color.WHITE)
            }
            FeedbackType.BONUS -> {
                binding.tvWordPreview.setBackgroundColor(Color.parseColor("#9D4EDD"))
                binding.tvWordPreview.setTextColor(Color.WHITE)
            }
            FeedbackType.ALREADY_FOUND -> {
                binding.tvWordPreview.setBackgroundColor(Color.parseColor("#FFB703"))
                binding.tvWordPreview.setTextColor(Color.BLACK)
                shakePreview()
            }
            FeedbackType.INVALID -> {
                binding.tvWordPreview.setBackgroundColor(Color.parseColor("#E63946"))
                binding.tvWordPreview.setTextColor(Color.WHITE)
                shakePreview()
            }
            FeedbackType.NONE -> {}
        }
    }

    private fun shakePreview() {
        ObjectAnimator.ofFloat(binding.tvWordPreview, "translationX", 0f, 25f, -25f, 20f, -20f, 0f).apply {
            duration = 400
            interpolator = CycleInterpolator(1f)
            start()
        }
    }

    private fun showPauseDialog() {
        val dialog = PauseDialogFragment().apply {
            soundEnabled = viewModel.uiState.value.soundEnabled
            hapticsEnabled = viewModel.uiState.value.hapticsEnabled
            listener = object : PauseDialogFragment.PauseDialogListener {
                override fun onResumeClicked() {}
                override fun onRestartClicked() {
                    viewModel.restartCurrentLevel()
                }
                override fun onLevelSelectClicked() {
                    parentFragmentManager.popBackStack()
                }
                override fun onToggleSound() {
                    viewModel.toggleSound()
                }
                override fun onToggleHaptics() {
                    viewModel.toggleHaptics()
                }
            }
        }
        dialog.show(parentFragmentManager, PauseDialogFragment.TAG)
    }

    private fun showLevelCompleteDialog(currentLevelId: Int, totalLevels: Int) {
        val dialog = LevelCompleteDialogFragment().apply {
            levelNumber = currentLevelId
            coinsEarned = 50
            hasNextLevel = currentLevelId < totalLevels
            listener = object : LevelCompleteDialogFragment.LevelCompleteListener {
                override fun onNextLevelClicked() {
                    viewModel.nextLevel()
                }
                override fun onLevelSelectClicked() {
                    parentFragmentManager.popBackStack()
                }
            }
        }
        dialog.show(parentFragmentManager, LevelCompleteDialogFragment.TAG)
    }

    private fun showBonusWordsDialog() {
        val state = viewModel.uiState.value
        val level = state.currentLevel ?: return
        val dialog = BonusWordsDialogFragment().apply {
            bonusWords = state.bonusWordsFound
            totalPossible = level.bonusWords.size
        }
        dialog.show(parentFragmentManager, BonusWordsDialogFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
