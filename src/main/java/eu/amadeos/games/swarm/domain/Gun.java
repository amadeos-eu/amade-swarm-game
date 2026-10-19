package eu.amadeos.games.swarm.domain;

/** Component: the insect may shoot at the fighter while it flies, this many times. */
public final class Gun {
    private int shots;
    private double wait;

    public Gun(int shots, double wait) {
        this.shots = shots;
        this.wait = wait;
    }

    /** Counts down; true when a shot is ready and left. */
    boolean ready(double seconds) {
        wait -= seconds;
        return shots > 0 && wait <= 0;
    }

    void fired(double pause) {
        shots--;
        wait = pause;
    }
}
