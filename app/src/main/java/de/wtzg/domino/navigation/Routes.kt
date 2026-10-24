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
