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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wtzg.domino.training.End
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

            // Board and messages scroll; the hand stays at the bottom.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BoardView(round, vm.lastPlaced)
                vm.aiNote?.let { NoteCard(CoachNote(NoteKind.INFO, "AI", it)) }
                vm.coachNote?.let { NoteCard(it) }
                vm.hint?.let { h ->
                    NoteCard(
                        CoachNote(
                            NoteKind.GOOD, "Hint: ${Strategy.describe(h.move, round.board)}",
                            h.pros.take(2).joinToString(" ").ifEmpty { "Any reasonable move works here." },
                        )
                    )
                }
                if (round.isOver) HandOver(vm, round)
            }

            HorizontalDivider()
            // Your hand and actions
            if (!round.isOver) {
                val status = when {
                    vm.aiThinking || round.turn == AI -> "AI is thinking…"
                    vm.selected != null -> "${vm.selected} fits both ends. Pick a side:"
                    round.canDraw -> "Nothing fits ${endsText(round)}. Draw a tile."
                    round.mustPass -> "Nothing fits and the boneyard is empty. You must pass."
                    else -> "Your turn. Tap a tile."
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                vm.selected?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(onClick = { vm.playSelected(End.LEFT) }) { Text("◀ Left (${round.board.leftEnd})") }
                        OutlinedButton(onClick = { vm.playSelected(End.RIGHT) }) { Text("Right (${round.board.rightEnd}) ▶") }
                    }
                }
            }
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                round.hands[YOU].forEach { t ->
                    val playable = legal.any { it.tile == t }
                    DominoTile(
                        t.a, t.b,
                        half = 24.dp,
                        highlighted = t == vm.selected || t == vm.hint?.move?.tile,
                        dimmed = yourTurn && !playable,
                        onClick = { vm.tapTile(t) },
                    )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoardView(round: Round, lastPlaced: de.wtzg.domino.training.Tile?) {
    Surface(
        color = Color(0xFF2E7D32),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            if (round.board.isEmpty) {
                Text("The table is empty.", color = Color.White)
            } else {
                Text(
                    "Open ends: ${round.board.leftEnd} (left) and ${round.board.rightEnd} (right)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    round.board.line.forEach { p ->
                        DominoTile(
                            p.left, p.right,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            vertical = p.tile.isDouble,
                            half = 17.dp,
                            highlighted = p.tile == lastPlaced,
                        )
                    }
                }
            }
        }
    }
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
