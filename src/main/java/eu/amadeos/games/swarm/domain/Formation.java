package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.math.Vector;

/**
 * Where the slots are right now. While the insects arrive, the formation sways left and right; once all are
 * there, it stops swaying, at the middle, and breathes instead: it spreads out and draws together. The
 * slots move, the insects in them move along.
 */
// --8<-- [start:formation]
public final class Formation {
    private double time;
    private boolean breathing;

    void update(double seconds, boolean complete) {
        double before = sway();
        time += seconds;
        // Switch only when the sway passes the middle, so no insect jumps.
        if (!breathing && complete && Math.signum(sway()) != Math.signum(before)) {
            breathing = true;
            time = 0;
        }
    }

    /** Back to swaying, for a new stage. */
    void reset() {
        time = 0;
        breathing = false;
    }

    public Vector slot(Slot slot) {
        double spread = breathing ? 1 + Field.BREATH * (1 - Math.cos(2 * Math.PI * time / Field.BREATH_SECONDS)) / 2 : 1;
        double x = Field.WIDTH / 2 + sway() + (slot.column() - (Field.COLUMNS - 1) / 2.0) * Field.SLOT_WIDTH * spread;
        double y = Field.FORMATION_TOP + slot.row() * Field.SLOT_HEIGHT * spread;
        return new Vector(x, y);
    }

    private double sway() {
        return breathing ? 0 : Field.SWAY * Math.sin(2 * Math.PI * time / Field.SWAY_SECONDS);
    }
    // --8<-- [end:formation]

    public boolean breathing() {
        return breathing;
    }
}
