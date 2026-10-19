package eu.amadeos.games.swarm.domain;

/** The field and the numbers of the game, in logical units and seconds. */
public final class Field {
    public static final double WIDTH = 640;
    public static final double HEIGHT = 400;

    // --8<-- [start:fighter]
    /** The fighter flies along this line at the bottom, left and right only. */
    public static final double FIGHTER_Y = 370;
    public static final double FIGHTER_SPEED = 230;
    /** How far the fighter's centre may go towards the edges. */
    public static final double FIGHTER_MARGIN = 16;
    /** Hit circles, a little smaller than the pictures. */
    public static final double FIGHTER_RADIUS = 6;
    public static final double INSECT_RADIUS = 7;
    /** Shots in the air at once: two, as in the arcade. */
    public static final int MAX_SHOTS = 2;
    public static final double SHOT_SPEED = 560;
    // --8<-- [end:fighter]
    /** The insects' shots: at most this many at once, in a pool of their own. */
    public static final int MAX_INSECT_SHOTS = 8;
    public static final double INSECT_SHOT_SPEED = 190;

    // --8<-- [start:formation]
    /** The formation: ten columns, five rows, around the top middle of the field. */
    public static final int COLUMNS = 10;
    public static final int ROWS = 5;
    public static final double SLOT_WIDTH = 28;
    public static final double SLOT_HEIGHT = 22;
    public static final double FORMATION_TOP = 50;
    /** While insects arrive, the formation sways left and right by this much, once in this many seconds. */
    public static final double SWAY = 24;
    public static final double SWAY_SECONDS = 5;
    /** When all have arrived, it breathes instead: its rows and columns spread by up to this part, and back. */
    public static final double BREATH = 0.14;
    public static final double BREATH_SECONDS = 3.5;
    // --8<-- [end:formation]

    // --8<-- [start:speeds]
    /** Speeds along flight paths, units per second, in the first stage; later stages are faster. */
    public static final double ENTRY_SPEED = 210;
    public static final double DIVE_SPEED = 170;
    public static final double HOMING_SPEED = 150;
    /** Seconds between two insects of a group on the same path: they follow each other in a line. */
    public static final double SPACING = 0.16;
    // --8<-- [end:speeds]

    public static final int START_LIVES = 3;
    public static final int MAX_LIVES = 9;
    public static final int FIRST_EXTRA_LIFE = 20_000;
    public static final int EXTRA_LIFE_EVERY = 60_000;
    /** Points per insect hit in a challenge stage, and the bonus if all are hit. */
    public static final int CHALLENGE_POINTS = 100;
    public static final int PERFECT_BONUS = 10_000;
    /** The stage's number on the screen before the first insects come. */
    public static final double STAGE_INTRO = 2.5;
    /** The pause after a cleared stage. */
    public static final double STAGE_BREAK = 2.0;
    /** After a crash, the next fighter waits at least this long, and then until no insect flies. */
    public static final double RESPAWN_SECONDS = 2.5;

    private Field() {
    }

    /** Stages get faster: 6 % per stage, at most two thirds more. */
    public static double pace(int stage) {
        return 1 + Math.min(stage - 1, 11) * 0.06;
    }

    /** Seconds between two dives from the formation: 3 in the first stage, at least 0.9. */
    public static double diveInterval(int stage) {
        return Math.max(0.9, 3.0 - 0.25 * (stage - 1));
    }

    /** Insects diving at the same time: 2 at first, one more every second stage, at most 6. */
    public static int maxDivers(int stage) {
        return Math.min(6, 2 + (stage - 1) / 2);
    }
}
