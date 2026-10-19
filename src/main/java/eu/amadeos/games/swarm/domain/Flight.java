package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.motion.FlightPath;

/** Component: the insect flies along a path, at an even speed, until its end. */
// --8<-- [start:flight]
public final class Flight {
    private final FlightPath path;
    private final double speed;
    private double distance;

    public Flight(FlightPath path, double speed) {
        this.path = path;
        this.speed = speed;
    }

    /** Flies on; true when the end of the path is reached. */
    boolean advance(double seconds) {
        distance += speed * seconds;
        return distance >= path.length();
    }
    // --8<-- [end:flight]

    public FlightPath path() {
        return path;
    }

    public double distance() {
        return distance;
    }
}
