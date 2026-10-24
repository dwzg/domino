package de.wtzg.domino.feature.play

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Top bar menu for a game against the computer, with a confirmed "Quit game". */
@Composable
fun QuitGameMenu(enabled: Boolean, onQuit: () -> Unit) {
    var menu by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
        DropdownMenuItem(
            text = { Text("Quit game") },
            enabled = enabled,
            onClick = { menu = false; confirm = true },
        )
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Quit this game?") },
            text = { Text("The match ends here and can't be resumed.") },
            confirmButton = { TextButton(onClick = { confirm = false; onQuit() }) { Text("Quit") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep playing") } },
        )
    }
}
