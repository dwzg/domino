package de.wtzg.domino.feature.play

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.wtzg.domino.engine.Match
import de.wtzg.domino.engine.Move
import de.wtzg.domino.engine.Round
import de.wtzg.domino.engine.Strategy
import de.wtzg.domino.engine.Tile
import de.wtzg.domino.ui.components.DominoTile
import de.wtzg.domino.ui.components.TableTarget
import de.wtzg.domino.ui.components.TableView
import kotlinx.coroutines.delay

/** Narrow rows in portrait so the line grows downwards and tiles stay big. */
private const val PORTRAIT_ROW = 4f
private const val LANDSCAPE_ROW = 7f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    actions: GameActions,
    haptics: Boolean,
    onBack: () -> Unit,
    onChangeGame: () -> Unit,
) {
    val match = state.match ?: return
    val round = match.round
    val legal = if (state.yourTurn) round.legalMoves() else emptyList()
    val title = "${match.rules.mode.title} · to ${match.rules.target}"

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.badge?.id) {
        if (haptics && state.badge != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val short = maxHeight < 480.dp
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (!landscape) {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        },
                        actions = {
                            TextButton(onClick = actions::showHint, enabled = state.yourTurn && legal.size > 1) { Text("Hint") }
                        },
                    )
                }
            },
        ) { padding ->
            val table = @Composable { m: Modifier ->
                Table(state, match, legal, actions, onChangeGame, if (landscape) LANDSCAPE_ROW else PORTRAIT_ROW, m)
            }
            if (landscape) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    table(Modifier.weight(1.5f).fillMaxHeight())
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = actions::showHint, enabled = state.yourTurn && legal.size > 1) { Text("Hint") }
                        }
                        Opponents(state, match, compact = short)
                        // Short screens get a two-line message box; tap it for the full text.
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            Messages(state, round, compact = short)
                        }
                        StatusRow(state, match, actions, compact = short)
                        Hand(state, round, legal, actions, compact = short)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp),
                ) {
                    Opponents(state, match)
                    table(Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp))
                    Messages(state, round)
                    StatusRow(state, match, actions)
                    Hand(state, round, legal, actions)
                }
            }
        }
    }
}

/** Each opponent with their score and tiles left; whoever is to move is highlighted. */
@Composable
private fun Opponents(state: GameUiState, match: Match, compact: Boolean = false) {
    val round = match.round
    val scores = match.scores
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (p in 1 until round.players) {
            val active = round.turn == p && !round.isOver
            Surface(
                color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = if (compact) 3.dp else 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            SEAT_NAMES[p],
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (active && state.aiThinking) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                        Text(" ${scores[p]}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 3.dp)) {
                        val hand = round.hands[p]
                        val shown = if (round.isOver) hand else null
                        val max = if (round.players > 2) 5 else 9
                        hand.take(max).forEachIndexed { i, _ ->
                            val t = shown?.get(i)
                            DominoTile(t?.a ?: 0, t?.b ?: 0, half = 8.dp, faceDown = t == null)
                        }
                        if (hand.size > max) Text("+${hand.size - max}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (match.rules.mode.drawsFromBoneyard) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 4.dp)) {
                DominoTile(0, 0, half = 9.dp, faceDown = true)
                Text("${round.boneyard.size}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Table(
    state: GameUiState,
    match: Match,
    legal: List<Move>,
    actions: GameActions,
    onChangeGame: () -> Unit,
    rowWidth: Float,
    modifier: Modifier,
) {
    val targets = when {
        !state.yourTurn -> emptyList()
        state.selected != null -> legal.filter { it.tile == state.selected }.map { TableTarget(it, isHint = false) }
        state.hint != null -> listOf(TableTarget(state.hint.move, isHint = true))
        else -> emptyList()
    }
    Box(modifier) {
        TableView(
            board = match.round.board,
            lastPlaced = state.lastPlaced,
            lastPlacedByOpponent = state.lastPlacedByOpponent,
            targets = targets,
            onTarget = actions::playMove,
            badge = state.badge,
            halfWidth = rowWidth,
            modifier = Modifier.fillMaxSize(),
        )
        if (match.isOver || match.round.isOver) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                if (match.isOver) MatchOver(match, actions, onChangeGame) else HandOver(state, match, actions)
            }
        }
    }
}

/**
 * One compact box: what the coach (or the hint) says about your play, and what the
 * others just did. Long texts are cut short; tap the box to read them in full.
 */
@Composable
private fun Messages(state: GameUiState, round: Round, compact: Boolean = false) {
    val hint = state.hint
    val coach: CoachNote? = when {
        hint != null -> CoachNote(
            NoteKind.GOOD, "Play ${Strategy.describe(hint.move, round.board)}.",
            hint.pros.take(2).joinToString(" ").ifEmpty { "Any reasonable move works here." } + " Tap the yellow spot to play it.",
        )
        else -> state.coach
    }
    val others = state.aiLog.joinToString(" ").ifEmpty { null }
    if (coach == null && others == null) return

    var showAll by remember { mutableStateOf(false) }
    var cut by remember(coach, others) { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showAll = true },
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        ) {
            coach?.let {
                MessageLine(if (hint != null) "Hint" else "Coach", noteColor(it.kind), it.title, it.text, maxLines = if (compact) 2 else 3, small = compact) { o ->
                    cut = cut || o
                }
            }
            others?.let {
                MessageLine("Table", MaterialTheme.colorScheme.onSurfaceVariant, "", it, maxLines = if (compact) 1 else 2, small = compact) { o ->
                    cut = cut || o
                }
            }
            if (cut && !compact) Text("Tap to read more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showAll) {
        AlertDialog(
            onDismissRequest = { showAll = false },
            confirmButton = { TextButton(onClick = { showAll = false }) { Text("OK") } },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    coach?.let {
                        Text(it.title, fontWeight = FontWeight.Bold)
                        Text(it.text)
                    }
                    if (state.aiLog.isNotEmpty()) {
                        Text("At the table", fontWeight = FontWeight.Bold)
                        state.aiLog.forEach { Text(it) }
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
private fun MessageLine(
    label: String,
    color: Color,
    title: String,
    text: String,
    maxLines: Int,
    small: Boolean = false,
    onOverflow: (Boolean) -> Unit,
) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier
                .widthIn(min = 48.dp)
                .padding(top = 2.dp, end = 8.dp),
        )
        Text(
            buildAnnotatedString {
                if (title.isNotEmpty()) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(title)
                        if (title.last() !in ".!?:") append(".")
                    }
                    append(" ")
                }
                append(text.replace('\n', ' '))
            },
            style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { onOverflow(it.hasVisualOverflow) },
        )
    }
}

/** Your score, what to do now, and Draw or Pass when that's the only option. */
@Composable
private fun StatusRow(state: GameUiState, match: Match, actions: GameActions, compact: Boolean = false) {
    val round = match.round
    val status = when {
        match.isOver || round.isOver -> ""
        state.aiThinking || round.turn != YOU -> "${SEAT_NAMES[round.turn]} is thinking…"
        state.selected != null -> "${state.selected} fits more than one end. Tap a glowing spot."
        round.canDraw -> "Nothing fits ${round.board.openValues.distinct().joinToString(" or ")}. Draw a tile."
        round.mustPass -> "Nothing fits. You have to pass."
        else -> "Your turn. Tap a tile."
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 40.dp else 48.dp)
            .padding(top = if (compact) 0.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = if (round.turn == YOU && !round.isOver) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                "You ${match.scores[YOU]}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (state.yourTurn && round.canDraw) Button(onClick = actions::draw) { Text("Draw") }
        if (state.yourTurn && round.mustPass) Button(onClick = actions::pass) { Text("Pass") }
    }
}

/** Your tiles in one row; they shrink a little when you hold many, and only wrap as a last resort. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hand(state: GameUiState, round: Round, legal: List<Move>, actions: GameActions, compact: Boolean = false) {
    val hand = round.hands[YOU]
    val gap = if (hand.size > 9) 4.dp else 6.dp
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 10.dp),
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
                        selected = t == state.selected,
                        hinted = t == state.hint?.move?.tile,
                        dimmed = state.yourTurn && legal.none { it.tile == t },
                        onClick = { actions.tapTile(t) },
                    )
                }
            }
        }
    }
}

/** A tile in your hand: deals in with a little stagger and lifts when selected. */
@Composable
private fun HandTile(tile: Tile, index: Int, half: Dp, selected: Boolean, hinted: Boolean, dimmed: Boolean, onClick: () -> Unit) {
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

@Composable
private fun OverlayCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 420.dp),
    ) {
        Column(
            Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
private fun ScoreLines(match: Match, pips: List<Int>?) {
    val scores = match.scores
    scores.indices.forEach { p ->
        Row(Modifier.fillMaxWidth()) {
            Text(SEAT_NAMES[p], modifier = Modifier.weight(1f), fontWeight = if (p == YOU) FontWeight.Bold else FontWeight.Normal)
            pips?.let { Text("${it[p]} pips  ·  ", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("${scores[p]}", fontWeight = FontWeight.Bold)
        }
    }
}

/** End-of-hand summary, shown on top of the table. */
@Composable
private fun HandOver(state: GameUiState, match: Match, actions: GameActions) {
    val res = match.round.result ?: return
    val winner = res.winner
    val title = when (winner) {
        null -> "Blocked. Nobody scores."
        YOU -> if (res.blocked) "Blocked. You win the hand!" else "Domino! You win the hand."
        else -> "${SEAT_NAMES[winner]} ${if (res.blocked) "wins the blocked hand." else "went out."}"
    }
    OverlayCard {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (winner != null) {
            val who = if (winner == YOU) "You score" else "${SEAT_NAMES[winner]} scores"
            val rounded = if (match.rules.mode.scoresDuringPlay) " (rounded to the nearest ${match.rules.mode.scoringMultiple})" else ""
            Text("$who ${res.endPoints} for the pips left in the other hands$rounded.")
        }
        ScoreLines(match, res.pipsLeft)
        if (state.handRated > 0) {
            Text(
                "You matched the coach on ${state.handMatched} of ${state.handRated} moves.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = actions::nextHand, modifier = Modifier.padding(top = 4.dp)) { Text("Next hand") }
    }
}

@Composable
private fun MatchOver(match: Match, actions: GameActions, onChangeGame: () -> Unit) {
    val w = match.winner ?: return
    OverlayCard {
        Text(
            if (w == YOU) "You win the match!" else "${SEAT_NAMES[w]} wins the match.",
            style = MaterialTheme.typography.headlineSmall,
        )
        if (!match.round.isOver) {
            Text("The last play took ${if (w == YOU) "you" else SEAT_NAMES[w]} past ${match.rules.target}.")
        }
        ScoreLines(match, null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(onClick = onChangeGame) { Text("Change game") }
            Button(onClick = actions::rematch) { Text("Rematch") }
        }
    }
}
