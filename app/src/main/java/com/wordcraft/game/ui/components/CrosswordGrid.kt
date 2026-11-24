package com.wordcraft.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordcraft.game.data.model.CellData
import com.wordcraft.game.ui.theme.CellEmptyBg
import com.wordcraft.game.ui.theme.CellEmptyBorder
import com.wordcraft.game.ui.theme.CellHintedBg
import com.wordcraft.game.ui.theme.CellRevealedBg
import com.wordcraft.game.ui.theme.CellRevealedText
import com.wordcraft.game.ui.theme.GoldenYellow

@Composable
fun CrosswordGrid(
    gridMatrix: List<List<CellData?>>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 320.dp
) {
    if (gridMatrix.isEmpty()) return

    val numRows = gridMatrix.size
    val numCols = gridMatrix.maxOfOrNull { it.size } ?: 1

    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth - 32.dp
        val availableHeight = (maxHeight - 20.dp).coerceAtLeast(100.dp)

        val cellSpacing = 5.dp
        val sizeBasedOnWidth = (availableWidth - (cellSpacing * (numCols - 1))) / numCols
        val sizeBasedOnHeight = (availableHeight - (cellSpacing * (numRows - 1))) / numRows

        val rawCellSize = minOf(sizeBasedOnWidth, sizeBasedOnHeight)
        val cellSize = rawCellSize.coerceIn(34.dp, 56.dp)

        Column(
            verticalArrangement = Arrangement.spacedBy(cellSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in 0 until numRows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rowCells = gridMatrix[r]
                    for (c in 0 until numCols) {
                        val cellData = rowCells.getOrNull(c)
                        if (cellData != null) {
                            GridCellItem(cellData = cellData, size = cellSize)
                        } else {
                            // Empty invisible placeholder
                            Box(modifier = Modifier.size(cellSize))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCellItem(
    cellData: CellData,
    size: Dp
) {
    val scale by animateFloatAsState(
        targetValue = if (cellData.isRevealed) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    val shape = RoundedCornerShape(8.dp)

    val backgroundColor = when {
        cellData.isRevealed && cellData.isHinted -> CellHintedBg
        cellData.isRevealed -> CellRevealedBg
        else -> CellEmptyBg
    }

    val borderColor = when {
        cellData.isRevealed && cellData.isHinted -> GoldenYellow
        cellData.isRevealed -> Color(0x33000000)
        else -> CellEmptyBorder
    }

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (cellData.isRevealed) 4.dp else 1.dp,
                shape = shape
            )
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.5.dp, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        if (cellData.isRevealed) {
            Text(
                text = cellData.letter.toString(),
                color = CellRevealedText,
                fontSize = (size.value * 0.52f).sp,
                fontWeight = FontWeight.Black
            )
            if (cellData.isHinted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(GoldenYellow)
                )
            }
        }
    }
}
