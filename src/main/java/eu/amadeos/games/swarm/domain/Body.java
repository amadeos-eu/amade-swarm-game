package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.math.Vector;

/**
 * Component: where an insect is and where it points, and where it was a step ago, so the painter can draw
 * it between the two. Every insect has one.
 */
// --8<-- [start:body]
public final class Body {
    private Vector position;
    private Vector previous;
    private double angle;
    private double previousAngle;

    public Body(Vector position, double angle) {
        jumpTo(position, angle);
    }

    /** Moves in one step: the old place becomes the previous one. */
    void moveTo(Vector position, double angle) {
        this.previous = this.position;
        this.previousAngle = this.angle;
        this.position = position;
        this.angle = angle;
    }

    /** Appears somewhere else, without a step in between to draw. */
    void jumpTo(Vector position, double angle) {
        this.position = previous = position;
        this.angle = previousAngle = angle;
    }
    // --8<-- [end:body]

    public Vector position() {
        return position;
    }

    public Vector previous() {
        return previous;
    }

    public double angle() {
        return angle;
    }

    public double previousAngle() {
        return previousAngle;
    }
}
