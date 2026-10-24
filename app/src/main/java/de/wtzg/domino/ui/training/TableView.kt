package de.wtzg.domino.ui.training

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.wtzg.domino.training.Board
import de.wtzg.domino.training.BoardLayout
import de.wtzg.domino.training.End
import de.wtzg.domino.training.Laid
import de.wtzg.domino.training.Move
import de.wtzg.domino.training.Tile
import kotlin.math.min
import kotlin.math.roundToInt

/** Size of half a tile before the table zooms to fit. */
private val UNIT = 24.dp

/** Largest zoom, so a near-empty table doesn't show giant tiles. */
private const val MAX_ZOOM = 1.5f

private val FeltCenter = Color(0xFF2F8A3A)
private val FeltEdge = Color(0xFF1B5E20)

/** A spot on the table the player can tap to play a tile there. */
data class TableTarget(val move: Move, val isHint: Boolean)

/**
 * The domino table. Tiles keep their place as the line grows; the camera pans and
 * zooms smoothly to keep everything in view, and the newest tile slides in.
 */
@Composable
fun TableView(
    board: Board,
    lastPlaced: Tile?,
    lastPlacedByAi: Boolean,
    targets: List<TableTarget>,
    onTarget: (Move) -> Unit,
    modifier: Modifier = Modifier,
    /** Row width in half-tiles; narrower rows suit tall (portrait) tables. */
    halfWidth: Float = BoardLayout.HALF_WIDTH,
) {
    val laid = remember(board, halfWidth) { BoardLayout.layout(board, halfWidth) }
    val slots = remember(board, targets, halfWidth) { targets.map { it to BoardLayout.slot(board, it.move, halfWidth) } }
    val shape = RoundedCornerShape(16.dp)

    BoxWithConstraints(
        modifier
            .clip(shape)
            .background(Brush.radialGradient(listOf(FeltCenter, FeltEdge))),
    ) {
        val density = LocalDensity.current
        val unitPx = with(density) { UNIT.toPx() }
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Fit all tiles plus the tap targets, with a margin.
        val all = laid + slots.map { it.second }
        val minX = all.minOfOrNull { it.x } ?: -1f
        val maxX = all.maxOfOrNull { it.x + it.w } ?: 1f
        val minY = all.minOfOrNull { it.y } ?: -1f
        val maxY = all.maxOfOrNull { it.y + it.h } ?: 1f
        val margin = 1.2f
        val fit = min(
            widthPx / ((maxX - minX + 2 * margin) * unitPx),
            heightPx / ((maxY - minY + 2 * margin) * unitPx),
        ).coerceAtMost(MAX_ZOOM)
        val camSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
        val zoom by animateFloatAsState(fit, camSpring, label = "zoom")
        val camX by animateFloatAsState((minX + maxX) / 2, camSpring, label = "camX")
        val camY by animateFloatAsState((minY + maxY) / 2, camSpring, label = "camY")

        // All tiles are positioned in table units around the box centre; one layer moves the camera.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = -camX * unitPx * zoom
                    translationY = -camY * unitPx * zoom
                },
        ) {
            val origin = IntOffset((widthPx / 2).roundToInt(), (heightPx / 2).roundToInt())
            laid.forEach { t ->
                key(t.tile) {
                    TableTile(t, origin, unitPx, animateIn = t.tile == lastPlaced, fromTop = lastPlacedByAi)
                }
            }
            slots.forEach { (target, slot) ->
                key(target) { TargetSpot(target, slot, origin, unitPx, onTarget) }
            }
        }

        if (board.isEmpty) {
            Text(
                "The table is empty",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val l = board.leftEnd
            val r = board.rightEnd
            Text(
                if (l == r) "Open ends: $l" else "Open ends: $l and $r",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            )
        }
    }
}

private fun Modifier.at(l: Laid, origin: IntOffset, unitPx: Float) = offset {
    IntOffset(origin.x + (l.x * unitPx).roundToInt(), origin.y + (l.y * unitPx).roundToInt())
}

@Composable
private fun TableTile(l: Laid, origin: IntOffset, unitPx: Float, animateIn: Boolean, fromTop: Boolean) {
    val progress = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    }
    val half: Dp = UNIT
    DominoTile(
        l.first, l.second,
        vertical = l.vertical,
        half = half,
        highlighted = animateIn,
        elevation = 3.dp,
        modifier = Modifier
            .at(l, origin, unitPx)
            .graphicsLayer {
                val p = progress.value
                val rest = 1f - p
                alpha = p.coerceIn(0f, 1f)
                translationY = rest * unitPx * 9f * (if (fromTop) -1f else 1f)
                rotationZ = rest * (if (fromTop) -30f else 30f)
                scaleX = 1f + rest * 0.5f
                scaleY = 1f + rest * 0.5f
            },
    )
}

@Composable
private fun TargetSpot(target: TableTarget, l: Laid, origin: IntOffset, unitPx: Float, onTap: (Move) -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val color = if (target.isHint) Color(0xFFFFD54F) else Color.White
    val shape = RoundedCornerShape(UNIT * 0.18f)
    val side = if (target.move.end == End.LEFT) "left" else "right"
    Box(
        Modifier
            .at(l, origin, unitPx)
            .size(UNIT * l.w, UNIT * l.h)
            .graphicsLayer { alpha = pulse }
            .clip(shape)
            .background(color.copy(alpha = 0.3f))
            .border(2.dp, color, shape)
            .clickable { onTap(target.move) }
            .semantics { contentDescription = "Play ${target.move.tile} on the $side" },
        contentAlignment = Alignment.Center,
    ) {
        Text("+", color = color, style = MaterialTheme.typography.titleLarge)
    }
}
