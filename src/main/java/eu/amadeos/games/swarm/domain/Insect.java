package eu.amadeos.games.swarm.domain;

/** Component: what kind of insect an entity is, and how many hits it still takes. */
public final class Insect {
    private final Kind kind;
    private int hits;

    public Insect(Kind kind) {
        this.kind = kind;
        this.hits = kind.hits();
    }

    public Kind kind() {
        return kind;
    }

    /** 2 for a fresh boss, then 1: the painter shows the boss's second colour. */
    public int hits() {
        return hits;
    }

    /** Takes a hit; true if it was the last. */
    boolean hit() {
        return --hits <= 0;
    }
}
