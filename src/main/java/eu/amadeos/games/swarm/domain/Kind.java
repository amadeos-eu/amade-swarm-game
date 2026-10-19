package eu.amadeos.games.swarm.domain;

import java.util.Locale;

/** The three kinds of insect, with their points in the formation and in flight, and the hits they take. */
// --8<-- [start:kind]
public enum Kind {
    BEE(50, 100, 1),
    BUTTERFLY(80, 160, 1),
    /** The boss takes two hits: the first only changes its colour. */
    BOSS(150, 400, 2);

    private final int points;
    private final int flyingPoints;
    private final int hits;

    Kind(int points, int flyingPoints, int hits) {
        this.points = points;
        this.flyingPoints = flyingPoints;
        this.hits = hits;
    }

    /** Points for an insect shot down; more if it was flying, not sitting in the formation. */
    public int points(boolean flying) {
        return flying ? flyingPoints : points;
    }

    public int hits() {
        return hits;
    }
    // --8<-- [end:kind]

    /** The kind a data file names: {@code bee}, {@code butterfly} or {@code boss}; null if none. */
    public static Kind named(String name) {
        for (Kind kind : values()) {
            if (kind.name().toLowerCase(Locale.ROOT).equals(name)) return kind;
        }
        return null;
    }
}
