package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.math.Vector;

/** Something that happened in a step; the user interface decides what it looks and sounds like. */
// --8<-- [start:event]
public sealed interface Event {
    record Fired(Vector at) implements Event {
    }

    record InsectFired(Vector at) implements Event {
    }

    /** An insect left the formation to dive at the fighter. */
    record Dived(Kind kind, Vector at) implements Event {
    }

    /** A boss took its first hit and changed its colour. */
    record InsectHit(Kind kind, Vector at) implements Event {
    }

    record InsectDestroyed(Kind kind, Vector at, int points) implements Event {
    }

    record FighterLost(Vector at) implements Event {
    }

    record FighterReady(Vector at) implements Event {
    }

    record StageStarted(int stage, boolean challenge) implements Event {
    }

    record StageCleared(int stage) implements Event {
    }

    /** The end of a challenge stage: insects hit of all, and the bonus. */
    record ChallengeOver(int hits, int of, int bonus) implements Event {
    }

    record ExtraLife(int lives) implements Event {
    }

    record GameOver(GameResult result) implements Event {
    }
}
// --8<-- [end:event]
