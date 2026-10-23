package de.wtzg.domino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import de.wtzg.domino.ui.GameScreen
import de.wtzg.domino.ui.GameViewModel
import de.wtzg.domino.ui.HistoryScreen
import de.wtzg.domino.ui.SetupScreen
import de.wtzg.domino.ui.theme.DominoTheme
import de.wtzg.domino.ui.training.LessonScreen
import de.wtzg.domino.ui.training.PracticeScreen
import de.wtzg.domino.ui.training.PracticeViewModel
import de.wtzg.domino.ui.training.TrainingHomeScreen

private const val SCORES = "scores"
private const val HISTORY = "history"
private const val TRAINING = "training"
private const val PRACTICE = "practice"
private const val LESSON = "lesson:"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DominoTheme { App() }
        }
    }
}

@Composable
private fun App() {
    val scoresVm: GameViewModel = viewModel()
    val practiceVm: PracticeViewModel = viewModel()
    var route by rememberSaveable { mutableStateOf(SCORES) }
    val topLevel = route == SCORES || route == TRAINING

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (topLevel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == SCORES,
                        onClick = { route = SCORES },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        label = { Text("Scores") },
                    )
                    NavigationBarItem(
                        selected = route == TRAINING,
                        onClick = { route = TRAINING },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        label = { Text("Training") },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            when {
                route == HISTORY -> {
                    BackHandler { route = SCORES }
                    HistoryScreen(
                        games = scoresVm.history,
                        onBack = { route = SCORES },
                        onDelete = scoresVm::deleteFromHistory,
                        onClear = scoresVm::clearHistory,
                    )
                }
                route == TRAINING -> TrainingHomeScreen(
                    vm = practiceVm,
                    onPlay = { route = PRACTICE },
                    onOpenLesson = { route = LESSON + it },
                )
                route == PRACTICE -> {
                    BackHandler { route = TRAINING }
                    if (practiceVm.round == null) route = TRAINING
                    PracticeScreen(practiceVm, onBack = { route = TRAINING })
                }
                route.startsWith(LESSON) -> {
                    val index = route.removePrefix(LESSON).toInt()
                    BackHandler { route = TRAINING }
                    key(index) {
                        LessonScreen(index, onBack = { route = TRAINING }, onOpen = { route = LESSON + it })
                    }
                }
                else -> {
                    val game = scoresVm.current
                    if (game == null) {
                        SetupScreen(
                            initialPlayers = scoresVm.lastPlayers,
                            onStart = scoresVm::startGame,
                            onShowHistory = { route = HISTORY },
                        )
                    } else {
                        GameScreen(
                            game = game,
                            onAddRound = scoresVm::addRound,
                            onUpdateRound = scoresVm::updateRound,
                            onDeleteRound = scoresVm::deleteRound,
                            onUndo = scoresVm::undoLastRound,
                            onRematch = scoresVm::rematch,
                            onNewGame = scoresVm::closeGame,
                            onShowHistory = { route = HISTORY },
                        )
                    }
                }
            }
        }
    }
}
