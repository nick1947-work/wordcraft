package com.wordcraft.game.ui.fragment

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
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
    private var isDragging = false

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
                isDragging = true
                viewModel.onLetterTouch(index)
            }

            override fun onWordUpdated(currentWord: String) {
                if (currentWord.isNotEmpty()) {
                    binding.tvWordPreview.visibility = View.VISIBLE
                    binding.tvWordPreview.text = currentWord
                    binding.tvWordPreview.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E6FFFFFF"))
                    binding.tvWordPreview.setTextColor(Color.parseColor("#0D1B2A"))
                } else {
                    binding.tvWordPreview.visibility = View.INVISIBLE
                }
            }

            override fun onWordCompleted(word: String) {
                isDragging = false
                if (word.length < 2) {
                    binding.tvWordPreview.text = ""
                    binding.tvWordPreview.visibility = View.INVISIBLE
                } else {
                    viewModel.submitWord(word)
                }
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
            if (!isDragging) {
                binding.tvWordPreview.text = ""
                binding.tvWordPreview.visibility = View.INVISIBLE
            }
            return
        }

        binding.tvWordPreview.visibility = View.VISIBLE
        binding.tvWordPreview.text = text

        val (bgColor, textColor, shouldShake) = when (type) {
            FeedbackType.VALID -> Triple("#52B788", Color.WHITE, false)
            FeedbackType.BONUS -> Triple("#9D4EDD", Color.WHITE, false)
            FeedbackType.ALREADY_FOUND -> Triple("#FFB703", Color.BLACK, true)
            FeedbackType.INVALID -> Triple("#E63946", Color.WHITE, true)
            FeedbackType.NONE -> return
        }
        binding.tvWordPreview.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        binding.tvWordPreview.setTextColor(textColor)
        if (shouldShake) shakePreview()
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
                    navigateToLevelSelect()
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
                    navigateToLevelSelect()
                }
            }
        }
        dialog.show(parentFragmentManager, LevelCompleteDialogFragment.TAG)
    }

    private fun navigateToLevelSelect() {
        val fm = parentFragmentManager
        val hasLevels = (0 until fm.backStackEntryCount).any {
            fm.getBackStackEntryAt(it).name == "levels"
        }
        if (hasLevels) {
            fm.popBackStack("levels", 0)
        } else {
            fm.popBackStack()
            fm.beginTransaction()
                .replace(R.id.fragment_container, LevelSelectFragment())
                .addToBackStack("levels")
                .commit()
        }
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
