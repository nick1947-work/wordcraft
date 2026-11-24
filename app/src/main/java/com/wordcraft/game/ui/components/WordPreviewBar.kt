package com.wordcraft.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.game.domain.FeedbackType
import com.wordcraft.game.ui.theme.BonusPurple
import com.wordcraft.game.ui.theme.ErrorRed
import com.wordcraft.game.ui.theme.GoldenYellow
import com.wordcraft.game.ui.theme.SuccessGreen
import kotlin.math.roundToInt

@Composable
fun WordPreviewBar(
    currentWord: String,
    feedbackType: FeedbackType,
    feedbackText: String,
    modifier: Modifier = Modifier
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(feedbackType) {
        if (feedbackType == FeedbackType.INVALID || feedbackType == FeedbackType.ALREADY_FOUND) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    -16f at 50 using FastOutLinearInEasing
                    16f at 100 using FastOutLinearInEasing
                    -12f at 150 using FastOutLinearInEasing
                    12f at 200 using FastOutLinearInEasing
                    -6f at 250 using FastOutLinearInEasing
                    6f at 300 using FastOutLinearInEasing
                    0f at 350
                }
            )
        }
    }

    val isShowingFeedback = feedbackType != FeedbackType.NONE && feedbackText.isNotEmpty()
    val displayText = if (isShowingFeedback) feedbackText else currentWord

    val bgColor = when (feedbackType) {
        FeedbackType.VALID -> SuccessGreen
        FeedbackType.BONUS -> BonusPurple
        FeedbackType.ALREADY_FOUND -> GoldenYellow
        FeedbackType.INVALID -> ErrorRed
        FeedbackType.NONE -> Color(0xD9FFFFFF)
    }

    val textColor = when (feedbackType) {
        FeedbackType.NONE -> Color(0xFF1B263B)
        FeedbackType.ALREADY_FOUND -> Color.Black
        else -> Color.White
    }

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = displayText.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bgColor)
                    .border(1.5.dp, Color(0x33000000), shape)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayText,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
