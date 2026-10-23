package de.wtzg.domino.ui.training

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wtzg.domino.training.Difficulty
import de.wtzg.domino.training.Lessons

private val TARGETS = listOf(50, 100, 150)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingHomeScreen(
    vm: PracticeViewModel,
    onPlay: () -> Unit,
    onOpenLesson: (Int) -> Unit,
) {
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.NORMAL) }
    var target by rememberSaveable { mutableStateOf(100) }
    var coach by rememberSaveable { mutableStateOf(true) }
    val tip = remember { Lessons.tips.random() }

    Scaffold(topBar = { TopAppBar(title = { Text("Training") }) }) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Play vs AI", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Draw dominoes, double-six, one on one. The coach rates each move and explains a better one.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            Difficulty.entries.forEachIndexed { i, d ->
                                SegmentedButton(
                                    selected = difficulty == d,
                                    onClick = { difficulty = d },
                                    shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                                ) { Text(d.label) }
                            }
                        }
                        Text(
                            difficulty.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Play to")
                            TARGETS.forEach { t ->
                                FilterChip(selected = target == t, onClick = { target = t }, label = { Text("$t") })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Coach feedback")
                                Text(
                                    "Comments on every move you make",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = coach, onCheckedChange = { coach = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                vm.start(PracticeSettings(difficulty, target, coach))
                                onPlay()
                            }) { Text("New match") }
                            if (vm.matchInProgress) {
                                OutlinedButton(onClick = onPlay) { Text("Continue") }
                            }
                        }
                    }
                }
            }

            item { StatsCard(vm) }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tip", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(tip)
                    }
                }
            }

            item { Text("Lessons", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
            itemsIndexed(Lessons.all) { i, lesson ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLesson(i) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            lesson.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(vm: PracticeViewModel) {
    val s = vm.stats
    if (s.movesRated == 0 && s.handsWon + s.handsLost == 0) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your progress", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::resetStats) { Text("Reset") }
            }
            s.accuracy?.let { StatRow("Moves matching the coach", "$it% (${s.movesMatched}/${s.movesRated})") }
            StatRow("Hands won / lost", "${s.handsWon} / ${s.handsLost}")
            StatRow("Matches won / lost", "${s.matchesWon} / ${s.matchesLost}")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}
