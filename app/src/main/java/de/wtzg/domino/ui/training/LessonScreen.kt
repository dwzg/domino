package de.wtzg.domino.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wtzg.domino.training.LessonBlock
import de.wtzg.domino.training.Lessons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LessonScreen(index: Int, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val lesson = Lessons.all[index]
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson.title) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            lesson.blocks.forEach { block ->
                when (block) {
                    is LessonBlock.Text -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
                    is LessonBlock.Tiles -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            block.tiles.forEach { DominoTile(it.a, it.b, half = 22.dp) }
                        }
                        Text(
                            block.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is LessonBlock.Line -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TableView(
                            board = block.board,
                            lastPlaced = null,
                            lastPlacedByAi = false,
                            targets = emptyList(),
                            onTarget = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                        )
                        Text(
                            block.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is LessonBlock.Tip -> Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Tip", fontWeight = FontWeight.Bold)
                            Text(block.text)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (index > 0) OutlinedButton(onClick = { onOpen(index - 1) }) { Text("Previous") }
                if (index < Lessons.all.lastIndex) Button(onClick = { onOpen(index + 1) }) { Text("Next lesson") }
            }
        }
    }
}
