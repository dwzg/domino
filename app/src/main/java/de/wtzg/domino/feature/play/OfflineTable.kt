package de.wtzg.domino.feature.play

import de.wtzg.domino.engine.Match

/** What the game screen shows for a game against the computer. */
fun GameUiState.toTable(): TableUi? {
    val m: Match = match ?: return null
    val r = m.round
    val scores = m.scores
    val yourTurn = this.yourTurn
    val status = when {
        m.isOver || r.isOver -> ""
        aiThinking || r.turn != YOU -> "${SEAT_NAMES[r.turn]} is thinking…"
        selected != null -> "$selected fits more than one end. Tap a glowing spot."
        r.canDraw -> "Nothing fits ${r.board.openValues.distinct().joinToString(" or ")}. Draw a tile."
        r.mustPass -> "Nothing fits. You have to pass."
        else -> "Your turn. Tap a tile."
    }
    return TableUi(
        title = "${m.rules.mode.title} · to ${m.rules.target}",
        board = r.board,
        hand = r.hands[YOU],
        legal = if (yourTurn) r.legalMoves() else emptyList(),
        yourTurn = yourTurn,
        canDraw = yourTurn && r.canDraw,
        mustPass = yourTurn && r.mustPass,
        yourScore = scores[YOU],
        opponents = (1 until r.players).map { p ->
            SeatUi(
                name = SEAT_NAMES[p],
                score = scores[p],
                tiles = r.hands[p].size,
                revealed = if (r.isOver) r.hands[p] else null,
                active = r.turn == p && !r.isOver,
                thinking = r.turn == p && aiThinking,
            )
        },
        boneyard = if (m.rules.mode.drawsFromBoneyard) r.boneyard.size else null,
        status = status,
        selected = selected,
        hint = hint,
        coach = coach,
        log = aiLog,
        lastPlaced = lastPlaced,
        lastPlacedByOpponent = lastPlacedByOpponent,
        badge = badge,
        overlay = overlay(m),
    )
}

private fun GameUiState.overlay(m: Match): OverlayUi? {
    val scores = m.scores
    val w = m.winner
    if (w != null) {
        return OverlayUi(
            title = if (w == YOU) "You win the match!" else "${SEAT_NAMES[w]} wins the match.",
            lines = if (!m.round.isOver) listOf("The last play took ${if (w == YOU) "you" else SEAT_NAMES[w]} past ${m.rules.target}.") else emptyList(),
            scores = scores.indices.map { ResultLine(SEAT_NAMES[it], null, scores[it], it == YOU) },
            primary = "Rematch",
            secondary = "Change game",
        )
    }
    val res = m.round.result ?: return null
    val winner = res.winner
    val title = when (winner) {
        null -> "Blocked. Nobody scores."
        YOU -> if (res.blocked) "Blocked. You win the hand!" else "Domino! You win the hand."
        else -> "${SEAT_NAMES[winner]} ${if (res.blocked) "wins the blocked hand." else "went out."}"
    }
    val lines = buildList {
        if (winner != null) {
            val who = if (winner == YOU) "You score" else "${SEAT_NAMES[winner]} scores"
            val rounded = if (m.rules.mode.scoresDuringPlay) " (rounded to the nearest ${m.rules.mode.scoringMultiple})" else ""
            add("$who ${res.endPoints} for the pips left in the other hands$rounded.")
        }
        if (handRated > 0) add("You matched the coach on $handMatched of $handRated moves.")
    }
    return OverlayUi(
        title = title,
        lines = lines,
        scores = scores.indices.map { ResultLine(SEAT_NAMES[it], res.pipsLeft[it], scores[it], it == YOU) },
        primary = "Next hand",
    )
}
