package de.wtzg.domino.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation destinations. Game modes travel as their enum name.

@Serializable object PlayRoute
@Serializable data class SetupRoute(val mode: String)
@Serializable object GameRoute
@Serializable object LearnRoute
@Serializable data class LessonRoute(val id: String)
@Serializable data class PuzzleRoute(val mode: String)
@Serializable object ScoresRoute
@Serializable object ScoreHistoryRoute
@Serializable object MoreRoute

/** Your online games. [code] comes from an invite link (dominoes://join/CODE). */
@Serializable data class OnlineRoute(val code: String? = null)
@Serializable data class JoinRoute(val code: String)
@Serializable object OnlineCreateRoute
/** One online game; also opened from notifications (dominoes://game/ID). */
@Serializable data class OnlineGameRoute(val id: String)
