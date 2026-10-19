package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.collision.Circle;
import eu.amadeos.gameengine.collision.Grid;
import eu.amadeos.gameengine.entity.Entities;
import eu.amadeos.gameengine.entity.Entity;
import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.gameengine.motion.FlightPath;
import eu.amadeos.gameengine.pool.Pool;
import eu.amadeos.gameengine.random.SeededRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A game of Swarm. Plain Java without a clock: {@link #update} advances everything by a step, with what the
 * player wants in that step, and the {@link #events()} tell what happened in it.
 *
 * <p>The insects are entities: each has a {@link Body} and an {@link Insect}, and a {@link Slot} if it
 * belongs in the formation. What it does follows from its other components: with a {@link Flight} it flies
 * along a path, with {@link Homing} it flies to its slot, with neither it sits in the formation, and with
 * a {@link Gun} it may shoot. Each step runs the <i>systems</i>, one per component, over all insects that
 * have it.
 *
 * <p>Chance (who dives when, who shoots) comes from a {@link SeededRandom} made from the seed, so a test can
 * play the same game twice.
 */
public final class Game {
    /** The fighter flies; the fighter is lost and the next waits; the last fighter is lost. */
    public enum Phase { PLAYING, WAITING, OVER }

    /** An insect still to come in this stage, and when. */
    private record Arrival(double time, FlightPath path, Stages.Member member) {
    }

    private final long seed;
    private final SeededRandom random;
    private final Stages stages;
    // --8<-- [start:fields]
    private final Entities insects = new Entities();
    private final Formation formation = new Formation();
    private final Grid<Entity> grid = new Grid<>(Field.WIDTH, Field.HEIGHT, 32);
    private final Pool<Shot> shots = new Pool<>(Field.MAX_SHOTS, Shot::new);
    private final Pool<Shot> insectShots = new Pool<>(Field.MAX_INSECT_SHOTS, Shot::new);
    // --8<-- [end:fields]
    private final List<Arrival> arrivals = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    /** The fighter, or {@code null} while it waits to appear. */
    private Fighter fighter = new Fighter(Field.WIDTH / 2);
    private Phase phase = Phase.PLAYING;
    private int score;
    private int lives = Field.START_LIVES;
    private int nextExtraLife = Field.FIRST_EXTRA_LIFE;
    private int stage;
    private Stages.Plan plan;
    private double stageTime;
    private double intro;
    private double stageBreak;
    private double untilDive;
    private double respawn;
    private int challengeHits;
    /** Set by a hit test, so the grid's visitor needs no object of its own. */
    private Entity struck;
    private Circle probe;

    public Game(long seed) {
        this(seed, Stages.STANDARD);
    }

    public Game(long seed, Stages stages) {
        this.seed = seed;
        this.random = new SeededRandom(seed);
        this.stages = stages;
        startStage(1);
    }

    public long seed() {
        return seed;
    }

    /** The fighter, or {@code null} while there is none. */
    public Fighter fighter() {
        return fighter;
    }

    /** All insects; read only. */
    public Entities insects() {
        return insects;
    }

    public Formation formation() {
        return formation;
    }

    /** The fighter's shots, {@link Pool#get} 0 to {@link Pool#size()} - 1. Read only. */
    public Pool<Shot> shots() {
        return shots;
    }

    public Pool<Shot> insectShots() {
        return insectShots;
    }

    public Phase phase() {
        return phase;
    }

    public int score() {
        return score;
    }

    /** Fighters left, counting the one flying. */
    public int lives() {
        return lives;
    }

    public int stage() {
        return stage;
    }

    public boolean challenge() {
        return plan.challenge();
    }

    /** Insects hit in this challenge stage. */
    public int challengeHits() {
        return challengeHits;
    }

    /** True while the stage's number is shown and no insect has come yet. */
    public boolean inIntro() {
        return intro > 0;
    }

    /** What happened in the last step. */
    public List<Event> events() {
        return Collections.unmodifiableList(events);
    }

    // --8<-- [start:update]
    /** One step of {@code seconds}: the fighter moves and shoots, the systems run, then what touches collides. */
    public void update(double seconds, Steering steering) {
        events.clear();
        if (fighter != null) {
            fighter.move(steering, seconds);
            if (steering.fire()) fire();
        }
        arrive(seconds);
        formation.update(seconds, arrivals.isEmpty() && flying() == 0);
        insects.forEach(Body.class, Flight.class, (insect, body, flight) -> fly(insect, body, flight, seconds));
        insects.forEach(Body.class, Homing.class, (insect, body, homing) -> home(insect, body, seconds));
        insects.forEach(Body.class, Slot.class, this::sit);
        dive(seconds);
        insects.forEach(Body.class, Gun.class, (insect, body, gun) -> shoot(body, gun, seconds));
        for (int i = 0; i < shots.size(); i++) shots.get(i).move(seconds);
        for (int i = 0; i < insectShots.size(); i++) insectShots.get(i).move(seconds);
        collide();
        shots.releaseIf(Shot::expired);
        insectShots.releaseIf(Shot::expired);
        insects.flush();
        waitForFighter(seconds);
        nextStage(seconds);
    }
    // --8<-- [end:update]

    private void fire() {
        Shot shot = shots.obtain();
        if (shot == null) return;     // two in the air: nothing happens
        shot.launch(fighter.nose(), new Vector(0, -Field.SHOT_SPEED));
        events.add(new Event.Fired(fighter.nose()));
    }

    // --8<-- [start:arrive]
    /** Insects whose time has come appear at the start of their path; with a slot, they will stay. */
    private void arrive(double seconds) {
        if (intro > 0) {
            intro -= seconds;
            return;
        }
        stageTime += seconds;
        while (!arrivals.isEmpty() && arrivals.getFirst().time() <= stageTime) {
            Arrival arrival = arrivals.removeFirst();
            FlightPath path = arrival.path();
            Entity insect = insects.create()
                    .add(new Body(path.start(), path.angleAt(0)))
                    .add(new Insect(arrival.member().kind()))
                    .add(new Flight(path, Field.ENTRY_SPEED * Field.pace(stage)));
            if (arrival.member().slot() != null) insect.add(arrival.member().slot());
            // From the second stage on, some insects shoot on their way in.
            if (!plan.challenge() && stage > 1 && random.nextDouble() < 0.3) {
                insect.add(new Gun(1, 0.4 + random.nextDouble()));
            }
        }
    }
    // --8<-- [end:arrive]

    // --8<-- [start:systems]
    /** The flight system: along the path; at its end, to the slot, or away. */
    private void fly(Entity insect, Body body, Flight flight, double seconds) {
        boolean done = flight.advance(seconds);
        FlightPath path = flight.path();
        body.moveTo(path.at(flight.distance()), path.angleAt(flight.distance()));
        if (!done) return;
        insect.remove(Flight.class);
        insect.remove(Gun.class);
        Slot slot = insect.get(Slot.class);
        if (slot == null) {
            insect.destroy();         // a challenge insect: through the field and gone
            return;
        }
        if (body.position().y() > Field.HEIGHT) {
            // Dived past the fighter and out at the bottom: it comes back in from above.
            body.jumpTo(new Vector(formation.slot(slot).x(), -16), Math.PI / 2);
        }
        insect.add(new Homing());
    }

    /** The homing system: straight towards the slot, which moves with the formation; there, it sits down. */
    private void home(Entity insect, Body body, double seconds) {
        Vector slot = formation.slot(insect.get(Slot.class));
        Vector way = slot.minus(body.position());
        double step = Field.HOMING_SPEED * Field.pace(stage) * seconds;
        if (way.length() <= step) {
            insect.remove(Homing.class);
            body.moveTo(slot, Math.PI / 2);
        } else {
            body.moveTo(body.position().plus(way.normalized().times(step)), way.angle());
        }
    }

    /** The formation system: an insect in its slot, neither flying nor homing, moves with the slot. */
    private void sit(Entity insect, Body body, Slot slot) {
        if (insect.has(Flight.class) || insect.has(Homing.class)) return;
        body.moveTo(formation.slot(slot), Math.PI / 2);
    }
    // --8<-- [end:systems]

    // --8<-- [start:dive]
    /**
     * Now and then an insect leaves the formation and dives at the fighter: it gets a flight, made here
     * for where it sits and where the fighter is, and a gun. Its slot stays its own.
     */
    private void dive(double seconds) {
        if (plan.challenge() || !arrivals.isEmpty() || phase != Phase.PLAYING || intro > 0) return;
        untilDive -= seconds;
        if (untilDive > 0 || flying() >= Field.maxDivers(stage)) return;
        untilDive = Field.diveInterval(stage) * (0.6 + 0.8 * random.nextDouble());
        List<Entity> sitting = new ArrayList<>();
        insects.forEach(Slot.class, (insect, slot) -> {
            if (!insect.has(Flight.class) && !insect.has(Homing.class)) sitting.add(insect);
        });
        if (sitting.isEmpty()) return;
        Entity diver = sitting.get(random.nextInt(sitting.size()));
        Vector from = diver.get(Body.class).position();
        diver.add(new Flight(divePath(from, fighter.x()), Field.DIVE_SPEED * Field.pace(stage)));
        diver.add(new Gun(2 + Math.min(stage / 2, 2), 0.2 + 0.4 * random.nextDouble()));
        events.add(new Event.Dived(diver.get(Insect.class).kind(), from));
    }

    /** A half loop up and outwards, then down towards the fighter, past it and out at the bottom. */
    static FlightPath divePath(Vector from, double target) {
        double side = from.x() < Field.WIDTH / 2 ? -1 : 1;
        double x = from.x();
        double y = from.y();
        return FlightPath.from(x, y)
                .curveTo(x, y - 36, x + side * 50, y - 36, x + side * 50, y)
                .curveTo(x + side * 50, y + 120, target - side * 90, 250, target, 310)
                .curveTo(target + side * 50, 350, target + side * 40, 390, target + side * 20, Field.HEIGHT + 24)
                .build();
    }
    // --8<-- [end:dive]

    /** Insects with a gun shoot at the fighter while it is below and in front of them. */
    private void shoot(Body body, Gun gun, double seconds) {
        if (!gun.ready(seconds) || fighter == null) return;
        Vector at = body.position();
        Vector aim = fighter.position().minus(at);
        if (at.y() < 60 || at.y() > Field.FIGHTER_Y - 90 || aim.y() < Math.abs(aim.x())) return;   // not too far aside
        Shot shot = insectShots.obtain();
        if (shot == null) return;
        shot.launch(at, aim.normalized().times(Field.INSECT_SHOT_SPEED * Field.pace(stage)));
        gun.fired(0.35);
        events.add(new Event.InsectFired(at));
    }

    // --8<-- [start:collide]
    /**
     * Every insect goes into the grid; each shot then only meets the insects in its cell and the ones next
     * to it, not all forty.
     */
    private void collide() {
        grid.clear();
        insects.forEach(Body.class, (insect, body) ->
                grid.add(insect, body.position().x(), body.position().y(), Field.INSECT_RADIUS));
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            probe = new Circle(shot.position().x(), shot.position().y(), 0);
            struck = null;
            grid.forEachNear(probe.x(), probe.y(), 0, this::test);
            if (struck != null) {
                shot.spend();
                hit(struck);
            }
        }
        if (fighter == null) return;
        Circle hull = new Circle(fighter.x(), Field.FIGHTER_Y, Field.FIGHTER_RADIUS);
        for (int i = 0; i < insectShots.size(); i++) {
            Shot shot = insectShots.get(i);
            if (hull.contains(shot.position().x(), shot.position().y())) {
                shot.spend();
                loseFighter();
                return;
            }
        }
        probe = hull;
        struck = null;
        grid.forEachNear(hull.x(), hull.y(), hull.radius(), this::test);
        if (struck != null) {
            destroy(struck);       // rammed: the insect is lost too, and still counts
            loseFighter();
        }
    }

    /** The grid's visitor: the first living insect that overlaps the probe is struck. */
    private void test(Entity insect) {
        if (struck != null || !insect.alive()) return;
        Vector at = insect.get(Body.class).position();
        if (probe.overlaps(new Circle(at.x(), at.y(), Field.INSECT_RADIUS))) struck = insect;
    }
    // --8<-- [end:collide]

    // --8<-- [start:hit]
    /** A boss takes two hits; everyone else falls at the first. */
    private void hit(Entity insect) {
        Insect what = insect.get(Insect.class);
        if (what.hit()) {
            destroy(insect);
        } else {
            events.add(new Event.InsectHit(what.kind(), insect.get(Body.class).position()));
        }
    }

    private void destroy(Entity insect) {
        Kind kind = insect.get(Insect.class).kind();
        boolean flying = insect.has(Flight.class) || insect.has(Homing.class);
        int points = plan.challenge() ? Field.CHALLENGE_POINTS : kind.points(flying);
        if (plan.challenge()) challengeHits++;
        insect.destroy();
        addPoints(points);
        events.add(new Event.InsectDestroyed(kind, insect.get(Body.class).position(), points));
    }
    // --8<-- [end:hit]

    private void addPoints(int points) {
        if (phase == Phase.OVER) return;    // the demo's insects fall on after the end
        score += points;
        if (score >= nextExtraLife) {
            nextExtraLife += nextExtraLife == Field.FIRST_EXTRA_LIFE
                    ? Field.EXTRA_LIFE_EVERY - Field.FIRST_EXTRA_LIFE : Field.EXTRA_LIFE_EVERY;
            if (lives < Field.MAX_LIVES) {
                lives++;
                events.add(new Event.ExtraLife(lives));
            }
        }
    }

    private void loseFighter() {
        events.add(new Event.FighterLost(fighter.position()));
        fighter = null;
        shots.clear();
        lives--;
        if (lives == 0) {
            phase = Phase.OVER;
            events.add(new Event.GameOver(new GameResult(score, stage)));
        } else {
            phase = Phase.WAITING;
            respawn = Field.RESPAWN_SECONDS;
        }
    }

    /** The next fighter comes when the wait is over and every diver is back in the formation. */
    private void waitForFighter(double seconds) {
        if (phase != Phase.WAITING) return;
        respawn -= seconds;
        boolean calm = plan.challenge() || (flying() == 0 && insectShots.size() == 0);
        if (respawn > 0 || !calm) return;
        fighter = new Fighter(Field.WIDTH / 2);
        phase = Phase.PLAYING;
        untilDive = Field.diveInterval(stage);
        events.add(new Event.FighterReady(fighter.position()));
    }

    /** Insects on a flight or on their way home: the ones that can still ram the fighter. */
    private int flying() {
        return insects.count(Flight.class) + insects.count(Homing.class);
    }

    // --8<-- [start:stage]
    /** When every insect of the stage is gone, a short break, and the next stage. */
    private void nextStage(double seconds) {
        if (!arrivals.isEmpty() || insects.count(Insect.class) > 0 || intro > 0) return;
        if (stageBreak == 0) {
            events.add(new Event.StageCleared(stage));
            if (plan.challenge()) {
                int of = plan.insects();
                int bonus = challengeHits == of ? Field.PERFECT_BONUS : 0;
                events.add(new Event.ChallengeOver(challengeHits, of, bonus));
                addPoints(bonus);
            }
        }
        stageBreak += seconds;
        if (stageBreak >= Field.STAGE_BREAK + (plan.challenge() ? 1.5 : 0)) startStage(stage + 1);
    }

    /** The stage's groups become a list of arrivals in time order; insects of a group follow each other. */
    private void startStage(int number) {
        stage = number;
        plan = stages.plan(number);
        stageTime = 0;
        stageBreak = 0;
        challengeHits = 0;
        intro = Field.STAGE_INTRO;
        untilDive = Field.diveInterval(number);
        formation.reset();
        arrivals.clear();
        double pace = Field.pace(number);
        for (Stages.Group group : plan.groups()) {
            for (int i = 0; i < group.members().size(); i++) {
                arrivals.add(new Arrival((group.time() + i * Field.SPACING) / pace, group.path(),
                        group.members().get(i)));
            }
        }
        arrivals.sort((a, b) -> Double.compare(a.time(), b.time()));    // stable: equal times keep their order
        events.add(new Event.StageStarted(number, plan.challenge()));
    }
    // --8<-- [end:stage]

    /** For tests: the insects still to come in this stage. */
    int waiting() {
        return arrivals.size();
    }

    /** For tests: clears the field and starts stage {@code number}. */
    void jumpToStage(int number) {
        insects.clear();
        startStage(number);
    }

    /** For tests: skips the stage's introduction. */
    void skipIntro() {
        intro = 0;
    }

    /** For tests: sets the score, e.g. just below an extra fighter. */
    void putScore(int score) {
        this.score = score;
    }
}
