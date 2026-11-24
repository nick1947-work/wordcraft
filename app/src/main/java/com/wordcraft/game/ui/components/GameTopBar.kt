package com.wordcraft.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.game.ui.theme.BrightGold
import com.wordcraft.game.ui.theme.GoldenYellow

@Composable
fun GameTopBar(
    levelNumber: Int,
    levelTitle: String,
    solvedCount: Int,
    totalCount: Int,
    coins: Int,
    bonusCount: Int,
    onPauseClick: () -> Unit,
    onBonusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Pause Button
        IconButton(
            onClick = onPauseClick,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x33000000))
                .border(1.dp, Color(0x44FFFFFF), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause Game",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Center: Level Title & Progress
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LEVEL $levelNumber",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "$levelTitle • $solvedCount/$totalCount",
                color = BrightGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Right side: Bonus words badge & Coins pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bonus Words Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x33000000))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                    .clickable { onBonusClick() }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Bonus Words",
                        tint = BrightGold,
                        modifier = Modifier.size(16.dp)
                    )
                    if (bonusCount > 0) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = bonusCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Coins Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, GoldenYellow, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🪙",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = coins.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
