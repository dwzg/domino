package de.wtzg.domino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import de.wtzg.domino.ui.GameScreen
import de.wtzg.domino.ui.GameViewModel
import de.wtzg.domino.ui.HistoryScreen
import de.wtzg.domino.ui.SetupScreen
import de.wtzg.domino.ui.theme.DominoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DominoTheme {
                val vm: GameViewModel = viewModel()
                var showHistory by rememberSaveable { mutableStateOf(false) }
                val game = vm.current

                when {
                    showHistory -> {
                        BackHandler { showHistory = false }
                        HistoryScreen(
                            games = vm.history,
                            onBack = { showHistory = false },
                            onDelete = vm::deleteFromHistory,
                            onClear = vm::clearHistory,
                        )
                    }
                    game == null -> SetupScreen(
                        initialPlayers = vm.lastPlayers,
                        onStart = vm::startGame,
                        onShowHistory = { showHistory = true },
                    )
                    else -> GameScreen(
                        game = game,
                        onAddRound = vm::addRound,
                        onUpdateRound = vm::updateRound,
                        onDeleteRound = vm::deleteRound,
                        onUndo = vm::undoLastRound,
                        onRematch = vm::rematch,
                        onNewGame = vm::closeGame,
                        onShowHistory = { showHistory = true },
                    )
                }
            }
        }
    }
}
