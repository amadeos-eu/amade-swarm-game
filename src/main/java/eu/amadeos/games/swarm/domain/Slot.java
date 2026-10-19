package eu.amadeos.games.swarm.domain;

/**
 * Component: the insect's place in the formation, column 0 to 9 from the left, row 0 to 4 from the top.
 * An insect without a slot, in a challenge stage, flies its path and leaves the field.
 */
public record Slot(int column, int row) {
}
