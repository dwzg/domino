package de.wtzg.domino.feature.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.wtzg.domino.data.SettingsRepository
import de.wtzg.domino.data.online.OnlineAccount
import de.wtzg.domino.data.online.OnlineException
import de.wtzg.domino.data.online.OnlineRepository
import de.wtzg.domino.protocol.CreateGameRequest
import de.wtzg.domino.protocol.GameSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnlineHomeState(
    val account: OnlineAccount? = null,
    val games: List<GameSummary> = emptyList(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    /** A game to open next (after creating or joining). Cleared once opened. */
    val open: String? = null,
)

/** What the online games screen can ask for. */
interface OnlineHomeActions {
    fun refresh()
    fun setServer(url: String)
    fun register(name: String)
    fun rename(name: String)
    fun join(code: String)
    fun opened()
    fun dismissError()
}

class OnlineHomeViewModel(
    private val repo: OnlineRepository,
    private val settings: SettingsRepository,
) : ViewModel(), OnlineHomeActions {
    private val _state = MutableStateFlow(OnlineHomeState())
    val state: StateFlow<OnlineHomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.account.distinctUntilChanged().collect { a ->
                _state.update { it.copy(account = a, games = if (a.registration == null) emptyList() else it.games) }
                if (a.registration != null) refresh()
            }
        }
    }

    override fun refresh() = launchSafely(busy = false) {
        _state.update { it.copy(loading = true) }
        try {
            val games = repo.api().games()
            _state.update { it.copy(games = games) }
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    override fun setServer(url: String) = launchSafely { settings.update { it.copy(serverUrl = url.trim()) } }

    override fun register(name: String) = launchSafely { repo.register(name) }

    override fun rename(name: String) = launchSafely { repo.rename(name) }

    override fun join(code: String) = launchSafely {
        val v = repo.api().join(code)
        _state.update { it.copy(open = v.id) }
    }

    fun create(request: CreateGameRequest) = launchSafely {
        val v = repo.api().create(request)
        _state.update { it.copy(open = v.id) }
    }

    override fun opened() = _state.update { it.copy(open = null) }
    override fun dismissError() = _state.update { it.copy(error = null) }

    /** Runs [block], turning server problems into a message on screen. */
    private fun launchSafely(busy: Boolean = true, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (busy) _state.update { it.copy(busy = true, error = null) }
            try {
                block()
            } catch (e: OnlineException) {
                _state.update { it.copy(error = e.message) }
            } finally {
                if (busy) _state.update { it.copy(busy = false) }
            }
        }
    }
}
