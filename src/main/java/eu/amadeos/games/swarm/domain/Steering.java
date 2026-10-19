package eu.amadeos.games.swarm.domain;

/**
 * What the player (or the computer) wants in one step. The rules never see keys or a mouse, only this.
 *
 * @param move   -1 left, +1 right, 0 still
 * @param target an x to move towards, for the mouse; {@code NaN} to use {@code move}
 * @param fire   a shot, if one of the two is left
 */
public record Steering(int move, double target, boolean fire) {
    public static final Steering NONE = new Steering(0, Double.NaN, false);
}
