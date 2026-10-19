package eu.amadeos.games.swarm.domain;

/** How a game ended: the points, and the stage the last fighter was lost in. */
public record GameResult(int score, int stage) {
}
