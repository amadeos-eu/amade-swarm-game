package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.math.Vector;

/**
 * A shot, the fighter's or an insect's. Shots live in pools, like the bullets of Rocks: the two of the
 * fighter are a rule of the game, the pool's capacity. {@link #launch} makes a shot like new.
 */
public final class Shot {
    private Vector position = Vector.ZERO;
    private Vector previous = Vector.ZERO;
    private Vector velocity = Vector.ZERO;
    private boolean spent;

    void launch(Vector position, Vector velocity) {
        this.position = position;
        this.previous = position;
        this.velocity = velocity;
        this.spent = false;
    }

    void move(double seconds) {
        previous = position;
        position = position.plus(velocity.times(seconds));
    }

    /** Used up on a target, or out of the field: back into the pool. */
    boolean expired() {
        return spent || position.y() < -12 || position.y() > Field.HEIGHT + 12
                || position.x() < -12 || position.x() > Field.WIDTH + 12;
    }

    void spend() {
        spent = true;
    }

    public Vector position() {
        return position;
    }

    public Vector previous() {
        return previous;
    }

    public Vector velocity() {
        return velocity;
    }
}
