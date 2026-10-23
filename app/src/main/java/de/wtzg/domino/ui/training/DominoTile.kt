package de.wtzg.domino.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val Ivory = Color(0xFFFBF8EF)
private val Pip = Color(0xFF263238)

/** Pip positions within one square half of a tile (x, y in 0..1). */
private val PIPS: Map<Int, List<Pair<Float, Float>>> = mapOf(
    0 to emptyList(),
    1 to listOf(.5f to .5f),
    2 to listOf(.27f to .27f, .73f to .73f),
    3 to listOf(.25f to .25f, .5f to .5f, .75f to .75f),
    4 to listOf(.27f to .27f, .73f to .27f, .27f to .73f, .73f to .73f),
    5 to listOf(.25f to .25f, .75f to .25f, .5f to .5f, .25f to .75f, .75f to .75f),
    6 to listOf(.27f to .22f, .27f to .5f, .27f to .78f, .73f to .22f, .73f to .5f, .73f to .78f),
)

/**
 * A domino drawn with pips. [first] is the top (vertical) or left (horizontal) half.
 */
@Composable
fun DominoTile(
    first: Int,
    second: Int,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    half: Dp = 28.dp,
    faceDown: Boolean = false,
    highlighted: Boolean = false,
    dimmed: Boolean = false,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(half * 0.18f)
    val back = MaterialTheme.colorScheme.primary
    val ring = if (highlighted) MaterialTheme.colorScheme.tertiary else Color(0xFF9E9E9E)
    Box(
        modifier
            .size(if (vertical) half else half * 2, if (vertical) half * 2 else half)
            .alpha(if (dimmed) 0.4f else 1f)
            .shadow(elevation, shape)
            .clip(shape)
            .background(if (faceDown) back else Ivory)
            .border(if (highlighted) 3.dp else 1.dp, ring, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = if (faceDown) "Hidden tile" else "$first-$second" },
    ) {
        if (!faceDown) {
            Canvas(Modifier.fillMaxSize()) {
                val h = if (vertical) size.width else size.height
                val r = h * 0.085f
                // Divider
                if (vertical) {
                    drawLine(Pip.copy(alpha = .5f), Offset(h * .15f, h), Offset(h * .85f, h), strokeWidth = h * .03f)
                } else {
                    drawLine(Pip.copy(alpha = .5f), Offset(h, h * .15f), Offset(h, h * .85f), strokeWidth = h * .03f)
                }
                listOf(first, second).forEachIndexed { i, n ->
                    val ox = if (vertical) 0f else h * i
                    val oy = if (vertical) h * i else 0f
                    PIPS[n]?.forEach { (x, y) ->
                        // Turn the pattern on horizontal tiles so a 6 shows two rows of three.
                        val (px, py) = if (vertical) x to y else y to x
                        drawCircle(Pip, r, Offset(ox + px * h, oy + py * h))
                    }
                }
            }
        }
    }
}
