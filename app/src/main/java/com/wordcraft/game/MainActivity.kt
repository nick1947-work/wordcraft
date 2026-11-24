package com.wordcraft.game

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordcraft.game.domain.GameUiEffect
import com.wordcraft.game.domain.Screen
import com.wordcraft.game.ui.screens.GameScreen
import com.wordcraft.game.ui.screens.HomeScreen
import com.wordcraft.game.ui.screens.LevelSelectScreen
import com.wordcraft.game.ui.theme.DarkBackgroundStart
import com.wordcraft.game.ui.theme.WordCraftTheme
import com.wordcraft.game.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WordCraftTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackgroundStart
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val context = LocalContext.current

                    // Handle system back navigation hygiene
                    BackHandler {
                        val handled = viewModel.handleBackPress()
                        if (!handled) {
                            finish()
                        }
                    }

                    // Collect and execute haptic effects
                    LaunchedEffect(Unit) {
                        viewModel.uiEffects.collect { effect ->
                            triggerHaptic(context, effect)
                        }
                    }

                    // Smooth screen transitions
                    AnimatedContent(
                        targetState = uiState.currentScreen,
                        transitionSpec = {
                            when {
                                targetState == Screen.GAMEPLAY -> {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                    )
                                }
                                initialState == Screen.GAMEPLAY -> {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut()
                                    )
                                }
                                else -> {
                                    fadeIn().togetherWith(fadeOut())
                                }
                            }
                        },
                        label = "screenTransition"
                    ) { screen ->
                        when (screen) {
                            Screen.HOME -> {
                                HomeScreen(
                                    currentLevel = uiState.currentLevel,
                                    coins = uiState.coins,
                                    onPlayClick = {
                                        viewModel.navigateTo(Screen.GAMEPLAY)
                                    },
                                    onLevelSelectClick = {
                                        viewModel.navigateTo(Screen.LEVEL_SELECT)
                                    }
                                )
                            }
                            Screen.LEVEL_SELECT -> {
                                LevelSelectScreen(
                                    levels = uiState.allLevels,
                                    highestUnlockedLevel = uiState.highestUnlockedLevel,
                                    onLevelSelected = { levelId ->
                                        viewModel.selectLevel(levelId)
                                    },
                                    onBackClick = {
                                        viewModel.navigateTo(Screen.HOME)
                                    }
                                )
                            }
                            Screen.GAMEPLAY -> {
                                GameScreen(
                                    state = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerHaptic(context: Context, effect: GameUiEffect) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                when (effect) {
                    GameUiEffect.VibrateLight -> {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    }
                    GameUiEffect.VibrateMedium -> {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    }
                    GameUiEffect.VibrateError -> {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                when (effect) {
                    GameUiEffect.VibrateLight -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(20)
                        }
                    }
                    GameUiEffect.VibrateMedium -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(45)
                        }
                    }
                    GameUiEffect.VibrateError -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(
                                VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 30), -1)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(longArrayOf(0, 30, 40, 30), -1)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore haptic failures on unsupported devices
        }
    }
}
