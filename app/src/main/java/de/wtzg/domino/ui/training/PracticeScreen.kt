package de.wtzg.domino.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import de.wtzg.domino.training.Tile
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wtzg.domino.training.Round
import de.wtzg.domino.training.Strategy

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                        Text("vs AI · ${settings.difficulty.label}")
                        Text(
                            "You ${vm.scores[YOU]} · AI ${vm.scores[AI]} · to ${settings.target}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            // Opponent
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text("AI · ${round.hands[AI].size} tiles", style = MaterialTheme.typography.labelLarge)
                if (vm.aiThinking) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                }
                Spacer(Modifier.weight(1f))
                Text("Boneyard: ${round.boneyard.size}", style = MaterialTheme.typography.labelLarge)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val shown = if (round.isOver) round.hands[AI] else null
                repeat(round.hands[AI].size) { i ->
                    val t = shown?.get(i)
                    DominoTile(t?.a ?: 0, t?.b ?: 0, half = 13.dp, faceDown = t == null)
                }
            }

            // The table takes the free space; notes sit below it.
            val targets = when {
                !yourTurn -> emptyList()
                vm.selected != null -> legal.filter { it.tile == vm.selected }.map { TableTarget(it, isHint = false) }
                vm.hint != null -> listOf(TableTarget(vm.hint!!.move, isHint = true))
                else -> emptyList()
            }
            TableView(
                board = round.board,
                lastPlaced = vm.lastPlaced,
                lastPlacedByAi = vm.lastPlacedBy == AI,
                targets = targets,
                onTarget = vm::playMove,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (round.isOver) 340.dp else 170.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (round.isOver) HandOver(vm, round)
                vm.hint?.let { h ->
                    NoteCard(
                        CoachNote(
                            NoteKind.GOOD, "Hint: ${Strategy.describe(h.move, round.board)}",
                            h.pros.take(2).joinToString(" ").ifEmpty { "Any reasonable move works here." } +
                                " Tap the yellow spot to play it.",
                        )
                    )
                }
                vm.coachNote?.let { NoteCard(it) }
                vm.aiNote?.let { NoteCard(CoachNote(NoteKind.INFO, "AI", it)) }
            }

            HorizontalDivider()
            // Your hand and actions
            if (!round.isOver) {
                val status = when {
                    vm.aiThinking || round.turn == AI -> "AI is thinking…"
                    vm.selected != null -> "${vm.selected} fits both ends. Tap a glowing spot on the table."
                    round.canDraw -> "Nothing fits ${endsText(round)}. Draw a tile."
                    round.mustPass -> "Nothing fits and the boneyard is empty. You must pass."
                    else -> "Your turn. Tap a tile."
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                round.hands[YOU].forEachIndexed { i, t ->
                    key(t) {
                        HandTile(
                            tile = t,
                            index = i,
                            selected = t == vm.selected,
                            hinted = t == vm.hint?.move?.tile,
                            dimmed = yourTurn && legal.none { it.tile == t },
                            onClick = { vm.tapTile(t) },
                        )
                    }
                }
            }
            if (!round.isOver) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    FilledTonalButton(onClick = vm::showHint, enabled = yourTurn && legal.size > 1) { Text("Hint") }
                    if (round.canDraw && yourTurn) Button(onClick = vm::draw) { Text("Draw") }
                    if (round.mustPass && yourTurn) Button(onClick = vm::pass) { Text("Pass") }
                }
            }
        }
    }
}

/** A tile in your hand: deals in with a little stagger and lifts when selected. */
@Composable
private fun HandTile(tile: Tile, index: Int, selected: Boolean, hinted: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
    }
    val lift by animateDpAsState(if (selected) (-12).dp else 0.dp, label = "lift")
    val density = LocalDensity.current
    DominoTile(
        tile.a, tile.b,
        half = 24.dp,
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
private fun NoteCard(note: CoachNote) {
    val (bg, fg) = when (note.kind) {
        NoteKind.GOOD -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        NoteKind.TIP -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        NoteKind.INFO -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (note.text.isNotEmpty()) Text(note.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

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
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

private fun endsText(round: Round): String {
    val l = round.board.leftEnd
    val r = round.board.rightEnd
    return if (l == r) "$l" else "$l or $r"
}
