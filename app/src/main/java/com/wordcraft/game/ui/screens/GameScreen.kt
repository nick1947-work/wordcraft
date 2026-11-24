package com.wordcraft.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.wordcraft.game.domain.GameUiState
import com.wordcraft.game.domain.Screen
import com.wordcraft.game.ui.components.ConfettiCelebration
import com.wordcraft.game.ui.components.CrosswordGrid
import com.wordcraft.game.ui.components.GameTopBar
import com.wordcraft.game.ui.components.HintButton
import com.wordcraft.game.ui.components.LetterWheel
import com.wordcraft.game.ui.components.ShuffleButton
import com.wordcraft.game.ui.components.WordPreviewBar
import com.wordcraft.game.ui.dialogs.BonusWordsDialog
import com.wordcraft.game.ui.dialogs.LevelCompleteDialog
import com.wordcraft.game.ui.dialogs.PauseDialog
import com.wordcraft.game.ui.theme.DarkBackgroundEnd
import com.wordcraft.game.ui.theme.DarkBackgroundStart
import com.wordcraft.game.ui.theme.NatureGreenMid
import com.wordcraft.game.ui.theme.NatureGreenStart
import com.wordcraft.game.viewmodel.GameViewModel

@Composable
fun GameScreen(
    state: GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val level = state.currentLevel ?: return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NatureGreenStart, NatureGreenMid, DarkBackgroundStart, DarkBackgroundEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            GameTopBar(
                levelNumber = level.id,
                levelTitle = level.title,
                solvedCount = state.solvedWordsCount,
                totalCount = state.totalWordsCount,
                coins = state.coins,
                bonusCount = state.bonusWordsFound.size,
                onPauseClick = { viewModel.pauseGame() },
                onBonusClick = { viewModel.toggleBonusDialog(true) }
            )

            // Crossword Grid (Top Area)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CrosswordGrid(
                    gridMatrix = state.gridMatrix,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Word Preview Bar (Center-Bottom)
            WordPreviewBar(
                currentWord = state.currentWord,
                feedbackType = state.feedbackType,
                feedbackText = state.feedbackText,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Controls Area: Shuffle Button, Letter Wheel, Hint Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button (Left)
                ShuffleButton(
                    onClick = { viewModel.shuffleWheel() }
                )

                // Circular Letter Wheel (Center)
                LetterWheel(
                    letters = state.wheelLetters,
                    selectedIndices = state.selectedIndices,
                    touchPosition = state.touchPosition,
                    shuffleAnimKey = state.shuffleAnimKey,
                    onLetterTouchDown = { idx, pos -> viewModel.onTouchDown(idx, pos) },
                    onLetterTouchMove = { pos, hovered -> viewModel.onTouchMove(pos, hovered) },
                    onLetterTouchUp = { viewModel.onTouchUp() }
                )

                // Hint Button (Right)
                HintButton(
                    cost = 25,
                    canAfford = state.coins >= 25,
                    onClick = { viewModel.useHint() }
                )
            }
        }

        // Particle Celebration
        ConfettiCelebration(
            triggerTimestamp = state.celebrationTimestamp
        )

        // Level Complete Dialog
        if (state.isLevelComplete) {
            val hasNext = level.id < state.allLevels.size
            LevelCompleteDialog(
                levelNumber = level.id,
                coinsEarned = 50,
                hasNextLevel = hasNext,
                onNextLevel = { viewModel.nextLevel() },
                onLevelSelect = { viewModel.navigateTo(Screen.LEVEL_SELECT) }
            )
        }

        // Pause Dialog
        if (state.isPaused) {
            PauseDialog(
                soundEnabled = state.soundEnabled,
                hapticsEnabled = state.hapticsEnabled,
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.restartCurrentLevel() },
                onLevelSelect = { viewModel.navigateTo(Screen.LEVEL_SELECT) },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() }
            )
        }

        // Bonus Words Dialog
        if (state.showBonusDialog) {
            BonusWordsDialog(
                bonusWordsFound = state.bonusWordsFound,
                totalBonusPossible = level.bonusWords.size,
                onDismiss = { viewModel.toggleBonusDialog(false) }
            )
        }
    }
}
