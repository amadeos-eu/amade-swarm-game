package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.pool.Pool;
import eu.amadeos.gameengine.random.SeededRandom;

/**
 * The computer player of the title screen. It moves under the lowest insect and fires when it is beneath
 * it, and it steps aside from shots and divers it sees coming. It sees them late, it looks away now and
 * then, as a person does, and its aim wanders a little: it plays well enough to watch and loses its
 * fighters in the end.
 */
public final class Pilot {
    /** The pilot pulls the trigger at most this often. */
    static final double FIRE_PAUSE = 0.25;
    /** How far above the fighter the pilot notices a shot or a diver. */
    static final double SIGHT = 80;
    /** The most the pilot's aim is off, in units, either way. */
    static final double SHAKE = 8;
    /** How often per second the pilot looks away, and for how long: it does not see what comes then. */
    static final double DISTRACTIONS = 0.5;
    static final double DISTRACTION = 0.35;

    /** Its own chance, apart from the game's. */
    private final SeededRandom random;
    private double sinceShot = FIRE_PAUSE;
    private double shake;
    private double distracted;

    public Pilot(long seed) {
        random = new SeededRandom(seed).fork();
    }

    // --8<-- [start:steer]
    public Steering steer(Game game, double seconds) {
        sinceShot += seconds;
        Fighter fighter = game.fighter();
        if (fighter == null) return Steering.NONE;
        distracted -= seconds;
        if (distracted <= 0 && random.nextDouble() < DISTRACTIONS * seconds) distracted = DISTRACTION;
        double dodge = distracted > 0 ? Double.NaN : danger(game, fighter);
        if (!Double.isNaN(dodge)) return new Steering(0, dodge, false);
        // The lowest insect on the field is the nearest threat and the easiest target.
        Vector[] lowest = {null};
        game.insects().forEach(Body.class, (insect, body) -> {
            Vector at = body.position();
            if (at.y() > 10 && at.y() < Field.FIGHTER_Y - 40 && (lowest[0] == null || at.y() > lowest[0].y())) {
                lowest[0] = at;
            }
        });
        if (lowest[0] == null) return new Steering(0, Field.WIDTH / 2, false);
        double aim = lowest[0].x() + shake;
        boolean fire = Math.abs(fighter.x() - aim) < 6 && sinceShot >= FIRE_PAUSE;
        if (fire) {
            sinceShot = 0;
            shake = (random.nextDouble() * 2 - 1) * SHAKE;
        }
        return new Steering(0, aim, fire);
    }

    /** Where to flee to from the nearest shot or diver about to reach the fighter; NaN if none. */
    private static double danger(Game game, Fighter fighter) {
        Pool<Shot> shots = game.insectShots();
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            double above = Field.FIGHTER_Y - shot.position().y();
            if (above <= 0 || above > SIGHT || shot.velocity().y() <= 0) continue;
            double arrivesAt = shot.position().x() + shot.velocity().x() * above / shot.velocity().y();
            if (Math.abs(arrivesAt - fighter.x()) < 16) return away(fighter.x(), arrivesAt);
        }
        double[] flee = {Double.NaN};
        game.insects().forEach(Body.class, Flight.class, (insect, body, flight) -> {
            Vector at = body.position();
            double above = Field.FIGHTER_Y - at.y();
            if (above > 0 && above < SIGHT && Math.abs(at.x() - fighter.x()) < 30) flee[0] = away(fighter.x(), at.x());
        });
        return flee[0];
    }

    private static double away(double x, double from) {
        double side = x < from ? -1 : 1;
        if (x < 60) side = 1;
        if (x > Field.WIDTH - 60) side = -1;
        return x + side * 60;
    }
    // --8<-- [end:steer]
}
