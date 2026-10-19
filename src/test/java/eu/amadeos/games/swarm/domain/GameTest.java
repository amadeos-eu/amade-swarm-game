package eu.amadeos.games.swarm.domain;

import eu.amadeos.gameengine.entity.Entity;
import eu.amadeos.gameengine.math.Vector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {
    private static final double STEP = 1.0 / 120;
    private static final Steering FIRE = new Steering(0, Double.NaN, true);

    /** Runs the game with the same steering until {@code done} or {@code seconds}; returns all events. */
    private static List<Event> runUntil(Game game, double seconds, Steering steering, BooleanSupplier done) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < Math.round(seconds / STEP) && !done.getAsBoolean(); i++) {
            game.update(STEP, steering);
            events.addAll(game.events());
        }
        return events;
    }

    private static List<Event> run(Game game, double seconds, Steering steering) {
        return runUntil(game, seconds, steering, () -> false);
    }

    /** Stays under {@code target}, which moves with the formation, and fires until it falls. */
    private static List<Event> hunt(Game game, Entity target, double seconds) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < Math.round(seconds / STEP) && target.alive(); i++) {
            game.update(STEP, new Steering(0, target.get(Body.class).position().x(), true));
            events.addAll(game.events());
        }
        return events;
    }

    private static long count(List<Event> events, Class<? extends Event> kind) {
        return events.stream().filter(kind::isInstance).count();
    }

    /** The insects sitting in the formation: a slot, and neither a flight nor homing. */
    private static List<Entity> sitting(Game game) {
        List<Entity> sitting = new ArrayList<>();
        game.insects().forEach(Slot.class, (insect, slot) -> {
            if (!insect.has(Flight.class) && !insect.has(Homing.class)) sitting.add(insect);
        });
        return sitting;
    }

    // --8<-- [start:test-arrive]
    @Test
    void fortyInsectsFlyInAndTakeTheirPlaces() {
        Game game = new Game(1);
        assertTrue(game.inIntro());
        run(game, Field.STAGE_INTRO + 0.6, Steering.NONE);
        assertEquals(8, game.insects().count(Flight.class), "the first two groups, one after the other");
        runUntil(game, 30, Steering.NONE, () -> sitting(game).size() == 40);
        assertEquals(40, sitting(game).size());
        assertEquals(0, game.waiting());
        for (Entity insect : sitting(game)) {
            Vector slot = game.formation().slot(insect.get(Slot.class));
            assertEquals(slot, insect.get(Body.class).position(), "in its slot");
        }
    }
    // --8<-- [end:test-arrive]

    // --8<-- [start:test-dive]
    @Test
    void aDiverChangesItsComponentsAndComesBack() {
        Game game = new Game(2);
        Entity[] diver = {null};
        runUntil(game, 60, Steering.NONE, () -> {
            for (Event event : game.events()) {
                if (event instanceof Event.Dived) {
                    game.insects().forEach(Gun.class, (insect, gun) -> {
                        if (insect.has(Slot.class)) diver[0] = insect;
                    });
                }
            }
            return diver[0] != null;
        });
        Entity insect = diver[0];
        assertNotNull(insect, "someone dived");
        assertTrue(insect.has(Flight.class) && insect.has(Gun.class), "flies, armed");
        runUntil(game, 20, Steering.NONE, () -> !insect.has(Flight.class) || !insect.alive());
        assertTrue(insect.has(Homing.class) || !insect.alive(), "on its way back, or it rammed the fighter");
        if (!insect.alive()) return;
        assertFalse(insect.has(Gun.class), "the gun is only for the dive");
        runUntil(game, 20, Steering.NONE, () -> !insect.has(Homing.class) || !insect.alive());
        if (insect.alive()) {
            assertEquals(game.formation().slot(insect.get(Slot.class)), insect.get(Body.class).position(),
                    "back in its slot, which was kept for it");
        }
    }
    // --8<-- [end:test-dive]

    @Test
    void twoShotsAtATime() {
        Game game = new Game(3);
        game.update(STEP, FIRE);
        game.update(STEP, FIRE);
        game.update(STEP, FIRE);
        assertEquals(2, game.shots().size());
        run(game, 1, Steering.NONE);
        assertEquals(0, game.shots().size(), "out at the top");
    }

    // --8<-- [start:test-boss]
    @Test
    void aBossTakesTwoHits() {
        Game game = new Game(4);
        runUntil(game, 30, Steering.NONE, () -> sitting(game).size() == 40);
        Entity boss = sitting(game).stream().filter(e -> e.get(Insect.class).kind() == Kind.BOSS).findFirst()
                .orElseThrow();
        // Under the boss, firing until it falls; whoever is in the way falls first.
        List<Event> events = hunt(game, boss, 30);
        assertFalse(boss.alive());
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.InsectHit(Kind kind, Vector at)
                && kind == Kind.BOSS), "the first hit only changed its colour");
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.InsectDestroyed(Kind kind, Vector at, int points)
                && kind == Kind.BOSS && (points == 150 || points == 400)));
    }
    // --8<-- [end:test-boss]

    // --8<-- [start:test-challenge]
    @Test
    void aChallengeStageFliesThroughAndCountsTheHits() {
        Game game = new Game(5);
        game.jumpToStage(3);
        assertTrue(game.challenge());
        List<Event> events = runUntil(game, 60, FIRE, () -> game.stage() == 4);
        Event.ChallengeOver over = events.stream().filter(Event.ChallengeOver.class::isInstance)
                .map(Event.ChallengeOver.class::cast).findFirst().orElseThrow();
        assertEquals(40, over.of());
        assertTrue(over.hits() > 0, "some flew into the shots");
        assertEquals(0, count(events, Event.InsectFired.class), "nobody shoots");
        assertEquals(0, count(events, Event.FighterLost.class));
        assertEquals(over.hits() * Field.CHALLENGE_POINTS + over.bonus(), game.score());
        assertEquals(over.hits() == 40 ? Field.PERFECT_BONUS : 0, over.bonus());
    }
    // --8<-- [end:test-challenge]

    @Test
    void aLostFighterWaitsForTheDiversAndTheLastEndsTheGame() {
        Game game = new Game(6);
        List<Event> events = runUntil(game, 10 * 60, Steering.NONE, () -> game.phase() == Game.Phase.OVER);
        assertEquals(Game.Phase.OVER, game.phase());
        assertEquals(Field.START_LIVES, count(events, Event.FighterLost.class));
        assertEquals(Field.START_LIVES - 1, count(events, Event.FighterReady.class));
        assertEquals(1, count(events, Event.GameOver.class));
        assertNull(game.fighter());
    }

    @Test
    void anExtraFighterAt20000Points() {
        Game game = new Game(7);
        game.putScore(Field.FIRST_EXTRA_LIFE - 50);
        runUntil(game, 30, Steering.NONE, () -> sitting(game).size() == 40);
        Entity bee = sitting(game).stream().filter(e -> e.get(Insect.class).kind() == Kind.BEE).findFirst()
                .orElseThrow();
        int lives = game.lives();
        List<Event> events = hunt(game, bee, 10);
        assertEquals(1, count(events, Event.ExtraLife.class));
        assertEquals(lives + 1 - count(events, Event.FighterLost.class), game.lives());
    }

    // --8<-- [start:test-determinism]
    @Test
    void theSameSeedAndPilotPlayTheSameGame() {
        Game one = new Game(42);
        Game two = new Game(42);
        Pilot first = new Pilot(42);
        Pilot second = new Pilot(42);
        for (int i = 0; i < 60 * 120; i++) {
            one.update(STEP, first.steer(one, STEP));
            two.update(STEP, second.steer(two, STEP));
        }
        assertEquals(one.score(), two.score());
        assertEquals(one.stage(), two.stage());
        assertEquals(one.insects().all().stream().map(e -> e.get(Body.class).position()).toList(),
                two.insects().all().stream().map(e -> e.get(Body.class).position()).toList());
    }
    // --8<-- [end:test-determinism]
}
