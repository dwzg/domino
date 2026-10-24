package de.wtzg.domino.ui.training

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.wtzg.domino.training.Move
import de.wtzg.domino.training.Round
import de.wtzg.domino.training.Strategy
import de.wtzg.domino.training.Tile
import kotlinx.coroutines.delay

/** Narrow rows in portrait so the line grows downwards and tiles stay big. */
private const val PORTRAIT_ROW = 4f
private const val LANDSCAPE_ROW = 7f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(vm: PracticeViewModel, onBack: () -> Unit) {
    val round = vm.round ?: return
    val settings = vm.settings ?: return
    val yourTurn = round.turn == YOU && !round.isOver && !vm.aiThinking
    val legal = if (yourTurn) round.legalMoves() else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("You ${vm.scores[YOU]} · AI ${vm.scores[AI]}")
                        Text(
                            "${settings.difficulty.label} · to ${settings.target}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = vm::showHint, enabled = yourTurn && legal.size > 1) { Text("Hint") }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            val landscape = maxWidth > maxHeight
            val table = @Composable { m: Modifier ->
                Table(vm, round, legal, yourTurn, if (landscape) LANDSCAPE_ROW else PORTRAIT_ROW, m)
            }
            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1.4f).fillMaxHeight()) {
                        OpponentStrip(vm, round)
                        table(Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp))
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Messages(vm, round)
                        StatusRow(vm, round, yourTurn)
                        Hand(vm, round, legal, yourTurn)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    OpponentStrip(vm, round)
                    table(Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp))
                    Messages(vm, round)
                    StatusRow(vm, round, yourTurn)
                    Hand(vm, round, legal, yourTurn)
                }
            }
        }
    }
}

/** The AI's tiles (face down until the hand ends) and the boneyard, in one row. */
@Composable
private fun OpponentStrip(vm: PracticeViewModel, round: Round) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("AI", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 6.dp))
        val hand = round.hands[AI]
        val shown = round.isOver
        // Show at most 9 tiles, then a count, so a big hand never wraps.
        hand.take(9).forEach { t ->
            DominoTile(t.a, t.b, half = 10.dp, faceDown = !shown)
        }
        if (hand.size > 9) Text("+${hand.size - 9}", style = MaterialTheme.typography.labelMedium)
        if (vm.aiThinking) {
            Spacer(Modifier.width(6.dp))
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
        }
        Spacer(Modifier.weight(1f))
        DominoTile(0, 0, half = 10.dp, faceDown = true)
        Text(
            " ×${round.boneyard.size}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun Table(
    vm: PracticeViewModel,
    round: Round,
    legal: List<Move>,
    yourTurn: Boolean,
    rowWidth: Float,
    modifier: Modifier,
) {
    val targets = when {
        !yourTurn -> emptyList()
        vm.selected != null -> legal.filter { it.tile == vm.selected }.map { TableTarget(it, isHint = false) }
        vm.hint != null -> listOf(TableTarget(vm.hint!!.move, isHint = true))
        else -> emptyList()
    }
    Box(modifier) {
        TableView(
            board = round.board,
            lastPlaced = vm.lastPlaced,
            lastPlacedByAi = vm.lastPlacedBy == AI,
            targets = targets,
            onTarget = vm::playMove,
            halfWidth = rowWidth,
            modifier = Modifier.fillMaxSize(),
        )
        if (round.isOver) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                HandOver(vm, round)
            }
        }
    }
}

/**
 * One compact box: what the coach (or the hint) says about your play, and what the
 * AI just did. Long texts are cut short; tap the box to read them in full.
 */
@Composable
private fun Messages(vm: PracticeViewModel, round: Round) {
    val hint = vm.hint
    val coach: CoachNote? = when {
        hint != null -> CoachNote(
            NoteKind.GOOD, "Play ${Strategy.describe(hint.move, round.board)}.",
            (hint.pros.take(2).joinToString(" ").ifEmpty { "Any reasonable move works here." }) +
                " Tap the yellow spot to play it.",
        )
        else -> vm.coachNote
    }
    val ai = vm.aiNote
    if (coach == null && ai == null) return

    var showAll by remember { mutableStateOf(false) }
    var cut by remember(coach, ai) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showAll = true },
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            coach?.let {
                MessageLine(if (hint != null) "Hint" else "Coach", noteColor(it.kind), it, maxLines = 3) { o -> cut = cut || o }
            }
            ai?.let {
                MessageLine("AI", MaterialTheme.colorScheme.onSurfaceVariant, CoachNote(NoteKind.INFO, "", it), maxLines = 2) { o ->
                    cut = cut || o
                }
            }
            if (cut) {
                Text(
                    "Tap to read more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (showAll) {
        AlertDialog(
            onDismissRequest = { showAll = false },
            confirmButton = { TextButton(onClick = { showAll = false }) { Text("OK") } },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    coach?.let {
                        Text(it.title, fontWeight = FontWeight.Bold)
                        Text(it.text)
                    }
                    ai?.let {
                        Text("AI", fontWeight = FontWeight.Bold)
                        Text(it)
                    }
                }
            },
        )
    }
}

@Composable
private fun noteColor(kind: NoteKind): Color = when (kind) {
    NoteKind.GOOD -> MaterialTheme.colorScheme.primary
    NoteKind.TIP -> MaterialTheme.colorScheme.tertiary
    NoteKind.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun MessageLine(label: String, color: Color, note: CoachNote, maxLines: Int, onOverflow: (Boolean) -> Unit) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier
                .widthIn(min = 44.dp)
                .padding(top = 2.dp, end = 8.dp),
        )
        Text(
            buildAnnotatedString {
                if (note.title.isNotEmpty()) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(note.title) }
                    append(" ")
                }
                append(note.text.replace('\n', ' '))
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { onOverflow(it.hasVisualOverflow) },
        )
    }
}

/** What to do now, with Draw or Pass right next to it when that's the only option. */
@Composable
private fun StatusRow(vm: PracticeViewModel, round: Round, yourTurn: Boolean) {
    if (round.isOver) return
    val status = when {
        vm.aiThinking || round.turn == AI -> "AI is thinking…"
        vm.selected != null -> "${vm.selected} fits both ends. Tap a glowing spot."
        round.canDraw -> "Nothing fits ${endsText(round)}."
        round.mustPass -> "Nothing fits and the boneyard is empty."
        else -> "Your turn. Tap a tile."
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (yourTurn && round.canDraw) Button(onClick = vm::draw) { Text("Draw") }
        if (yourTurn && round.mustPass) Button(onClick = vm::pass) { Text("Pass") }
    }
}

/** Your tiles in one row; they shrink a little when you hold many, and only wrap as a last resort. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hand(vm: PracticeViewModel, round: Round, legal: List<Move>, yourTurn: Boolean) {
    val hand = round.hands[YOU]
    val gap = if (hand.size > 9) 4.dp else 6.dp
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 10.dp),
    ) {
        val n = hand.size.coerceAtLeast(1)
        // 1dp of slack so rounding never pushes the last tile onto a second row.
        val half: Dp = ((maxWidth - 1.dp - gap * (n - 1)) / n).coerceIn(18.dp, 28.dp)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            hand.forEachIndexed { i, t ->
                key(t) {
                    HandTile(
                        tile = t,
                        index = i,
                        half = half,
                        selected = t == vm.selected,
                        hinted = t == vm.hint?.move?.tile,
                        dimmed = yourTurn && legal.none { it.tile == t },
                        onClick = { vm.tapTile(t) },
                    )
                }
            }
        }
    }
}

/** A tile in your hand: deals in with a little stagger and lifts when selected. */
@Composable
private fun HandTile(
    tile: Tile,
    index: Int,
    half: Dp,
    selected: Boolean,
    hinted: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }
    val lift by animateDpAsState(if (selected) (-10).dp else 0.dp, label = "lift")
    val density = LocalDensity.current
    DominoTile(
        tile.a, tile.b,
        half = half,
        highlighted = selected || hinted,
        dimmed = dimmed,
        elevation = if (selected) 8.dp else 2.dp,
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            val p = appear.value
            alpha = p.coerceIn(0f, 1f)
            translationY = with(density) { lift.toPx() } + (1f - p) * 60f
            scaleX = 0.6f + 0.4f * p
            scaleY = 0.6f + 0.4f * p
        },
    )
}

private fun endsText(round: Round): String {
    val l = round.board.leftEnd
    val r = round.board.rightEnd
    return if (l == r) "$l" else "$l or $r"
}

/** End-of-hand summary, shown on top of the table. */
@Composable
private fun HandOver(vm: PracticeViewModel, round: Round) {
    val res = round.result ?: return
    val title = when (res.winner) {
        YOU -> if (res.blocked) "Blocked. You win the hand." else "Domino! You win the hand."
        AI -> if (res.blocked) "Blocked. The AI wins the hand." else "The AI went out."
        else -> "Blocked. It's a tie."
    }
    val detail = buildString {
        append("Pips left: you ${res.pipsLeft[YOU]}, AI ${res.pipsLeft[AI]}.")
        if (res.winner != null) append(" ${if (res.winner == YOU) "You score" else "The AI scores"} ${res.points}.")
        if (vm.handRated > 0) append("\nYou matched the coach on ${vm.handMatched} of ${vm.handRated} moves.")
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 420.dp),
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail)
            vm.matchWinner?.let {
                Text(
                    if (it == YOU) "You win the match ${vm.scores[YOU]} to ${vm.scores[AI]}!"
                    else "The AI wins the match ${vm.scores[AI]} to ${vm.scores[YOU]}.",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Button(onClick = vm::nextHand) { Text(if (vm.matchWinner != null) "New match" else "Next hand") }
        }
    }
}
