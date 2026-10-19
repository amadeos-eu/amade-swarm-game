package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.math.Vector;

/** The player's fighter: one of a kind, so a plain class, not an entity. It moves along the bottom. */
public final class Fighter {
    private double x;
    private double previousX;

    Fighter(double x) {
        this.x = previousX = x;
    }

    // --8<-- [start:move]
    /** Moves left or right by the steering, or towards a point it aims at (the mouse), never past the edges. */
    void move(Steering steering, double seconds) {
        previousX = x;
        double step = Field.FIGHTER_SPEED * seconds;
        if (!Double.isNaN(steering.target())) {
            x += Math.clamp(steering.target() - x, -step, step);
        } else {
            x += steering.move() * step;
        }
        x = Math.clamp(x, Field.FIGHTER_MARGIN, Field.WIDTH - Field.FIGHTER_MARGIN);
    }
    // --8<-- [end:move]

    public double x() {
        return x;
    }

    public double previousX() {
        return previousX;
    }

    public Vector position() {
        return new Vector(x, Field.FIGHTER_Y);
    }

    /** Where shots leave: the tip of the nose. */
    Vector nose() {
        return new Vector(x, Field.FIGHTER_Y - 9);
    }
}
