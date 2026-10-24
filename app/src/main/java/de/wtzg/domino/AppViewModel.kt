package de.wtzg.domino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.wtzg.domino.data.Settings
import de.wtzg.domino.data.SettingsRepository
import de.wtzg.domino.data.Stats
import de.wtzg.domino.data.StatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide state: settings (null until loaded, so the splash screen can wait for the theme) and stats. */
class AppViewModel(
    private val settingsRepo: SettingsRepository,
    private val statsRepo: StatsRepository,
) : ViewModel() {
    val settings: StateFlow<Settings?> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val stats: StateFlow<Stats> = statsRepo.stats.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    fun updateSettings(transform: (Settings) -> Settings) = viewModelScope.launch { settingsRepo.update(transform) }
    fun resetStats() = viewModelScope.launch { statsRepo.reset() }
    fun markLessonRead(id: String) = viewModelScope.launch {
        statsRepo.update { if (id in it.lessonsRead) it else it.copy(lessonsRead = it.lessonsRead + id) }
    }
}
