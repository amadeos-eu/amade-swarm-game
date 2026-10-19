package eu.amadeos.games.swarm.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.games.swarm.bootstrap.SwarmModule;
import eu.amadeos.games.swarm.domain.Field;
import eu.amadeos.games.swarm.domain.Game;
import eu.amadeos.games.swarm.domain.GameResult;
import eu.amadeos.games.swarm.domain.Insect;
import eu.amadeos.games.swarm.domain.Kind;
import eu.amadeos.games.swarm.repository.SwarmSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys and moves the mouse, and
 * calls the steps itself, so everything happens in order on the test thread.
 */
class SwarmGameTest {
    private static final double STEP = SwarmGame.STEP;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final Mouse mouse = new Mouse();
    private final List<GameResult> results = new ArrayList<>();
    private SwarmGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(SwarmModule.class, SwarmModule.MESSAGES, Locale.ENGLISH);
        game = new SwarmGame(keyboard, mouse, msg, new SwarmSettings(dir.resolve("swarm.properties")), () -> 77L,
                new SplittableRandom(4), new Mixer(), Runnable::run);
        game.onGameOver(results::add);
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(1);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private void seconds(double seconds) {
        steps((int) Math.round(seconds * SwarmGame.UPDATES_PER_SECOND));
    }

    private SwarmGame.PlayScene play() {
        tap(KeyEvent.VK_ENTER);
        return assertInstanceOf(SwarmGame.PlayScene.class, game.top());
    }

    @Test
    void enterStartsAGameAndSpaceFires() {
        assertInstanceOf(SwarmGame.TitleScene.class, game.top());
        SwarmGame.PlayScene play = play();
        assertEquals(0, play.game().shots().size());
        tap(KeyEvent.VK_SPACE);
        assertEquals(1, play.game().shots().size());
    }

    @Test
    void theKeysMoveTheFighter() {
        SwarmGame.PlayScene play = play();
        keyboard.pressed(KeyEvent.VK_LEFT, now[0]);
        seconds(0.5);
        assertEquals(Field.WIDTH / 2 - Field.FIGHTER_SPEED * 0.5, play.game().fighter().x(), 1);
    }

    // --8<-- [start:test-mouse]
    @Test
    void theFighterFollowsThePointerUntilAKeyMovesIt() {
        SwarmGame.PlayScene play = play();
        mouse.moved(500, 200);
        seconds(1);
        assertTrue(play.mouseSteers());
        assertEquals(500, play.game().fighter().x(), 1e-9, "under the pointer");
        mouse.pressed(MouseEvent.BUTTON1);
        steps(1);
        mouse.released(MouseEvent.BUTTON1);
        assertEquals(1, play.game().shots().size(), "a click fires");
        keyboard.pressed(KeyEvent.VK_LEFT, now[0]);
        seconds(0.5);
        assertFalse(play.mouseSteers(), "a key takes over again");
        assertEquals(500 - Field.FIGHTER_SPEED * 0.5, play.game().fighter().x(), 1);
    }
    // --8<-- [end:test-mouse]

    @Test
    void pFreezesTheGameAndAClickContinuesIt() {
        SwarmGame.PlayScene play = play();
        seconds(Field.STAGE_INTRO + 1);
        tap(KeyEvent.VK_P);
        assertEquals(2, game.sceneCount());
        int insects = play.game().insects().count(Insect.class);
        double x = play.game().insects().all().getFirst().get(eu.amadeos.games.swarm.domain.Body.class).position().x();
        seconds(1);
        assertEquals(insects, play.game().insects().count(Insect.class));
        assertEquals(x, play.game().insects().all().getFirst()
                .get(eu.amadeos.games.swarm.domain.Body.class).position().x(), "frozen");
        mouse.pressed(MouseEvent.BUTTON1);
        steps(1);
        mouse.released(MouseEvent.BUTTON1);
        seconds(0.5);
        assertNotEquals(x, play.game().insects().all().getFirst()
                .get(eu.amadeos.games.swarm.domain.Body.class).position().x(), "goes on");
    }

    @Test
    void aFighterThatDoesNothingIsLostAndTheResultReachesThePresenter() {
        SwarmGame.PlayScene play = play();
        for (int i = 0; i < 20 * 60 * 120 && results.isEmpty(); i++) steps(1);
        assertEquals(1, results.size());
        assertEquals(Game.Phase.OVER, play.game().phase());
        assertInstanceOf(SwarmGame.OverScene.class, game.top());
        seconds(2);
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(SwarmGame.TitleScene.class, game.top());
    }

    @Test
    void theDemoShootsInsectsAndTheyExplode() {
        boolean exploded = false;
        for (int i = 0; i < 40 * 120 && !exploded; i++) {
            steps(1);
            exploded = game.effects().particles().count() > 0;
        }
        assertTrue(exploded, "an insect was hit");
        SwarmGame.TitleScene title = assertInstanceOf(SwarmGame.TitleScene.class, game.top());
        assertTrue(title.demo().score() > 0);
    }

    @Test
    void anInsectsDirectionPicksAColumnOfTheSheet() {
        assertEquals(0, SwarmPainter.column(0));
        assertEquals(4, SwarmPainter.column(Math.PI / 2), "down");
        assertEquals(12, SwarmPainter.column(-Math.PI / 2), "up");
        assertEquals(8, SwarmPainter.column(Math.PI));
        assertEquals(8, SwarmPainter.column(-Math.PI + 0.01));
        assertEquals(1, SwarmPainter.column(Math.toRadians(20)));
    }

    @Test
    void everySceneDrawsWorldAndOverlay() {
        BufferedImage image = new BufferedImage((int) Field.WIDTH, (int) Field.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        seconds(2);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        SwarmGame.PlayScene play = play();
        game.render(g, 0.5);
        int x = (int) Math.round(play.game().fighter().x());
        assertEquals(0xF4F4FF, image.getRGB(x, (int) Field.FIGHTER_Y - 5) & 0xFFFFFF, "the fighter's white nose");
        game.renderOverlay(g, 0.5);
        seconds(Field.STAGE_INTRO + 3);
        game.render(g, 0.5);
        tap(KeyEvent.VK_P);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        g.dispose();
        assertTrue(play.game().insects().count(Insect.class) > 0);
        assertEquals(Kind.BUTTERFLY, play.game().insects().all().getFirst().get(Insect.class).kind());
    }
}
