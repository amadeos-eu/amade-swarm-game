package eu.amadeos.games.swarm.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.swarm.domain.Event;
import eu.amadeos.games.swarm.domain.Game;
import eu.amadeos.games.swarm.domain.GameResult;
import eu.amadeos.games.swarm.domain.Pilot;
import eu.amadeos.games.swarm.domain.Steering;
import eu.amadeos.games.swarm.repository.SwarmSettings;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/**
 * The running game: the loop, the controls, the effects and the scenes (title with the computer playing, a
 * game, the pause screen, the end of a game). Everything here runs on the game loop thread, except the
 * {@link Arena} methods, which only post commands to it.
 */
public final class SwarmGame implements Arena {
    /** The actions of the game; which keys trigger them comes from the settings. */
    public enum Action { LEFT, RIGHT, FIRE, PAUSE, START }

    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;
    static final double STEP = 1.0 / UPDATES_PER_SECOND;

    private final Keyboard keyboard;
    private final Mouse mouse;
    private final Controls<Action> controls;
    private final Messages msg;
    private final LongSupplier seeds;
    private final Executor ui;
    private final Mixer mixer;
    private final SwarmPainter painter = new SwarmPainter();
    private final Hud hud;
    private final Effects effects;
    private final SceneStack scenes = new SceneStack();
    private volatile Consumer<GameResult> onGameOver = result -> { };
    private GameLoop loop;

    @Inject
    public SwarmGame(Keyboard keyboard, Mouse mouse, Messages msg, SwarmSettings settings, RandomGenerator looks,
                     Mixer mixer, EdtExecutor ui) {
        this(keyboard, mouse, msg, settings, SeededRandom::freshSeed, looks, mixer, (Executor) ui);
    }

    /**
     * @param seeds the seed of each new game
     * @param looks chance for the stars and sparks, apart from the rules' own
     */
    SwarmGame(Keyboard keyboard, Mouse mouse, Messages msg, SwarmSettings settings, LongSupplier seeds,
              RandomGenerator looks, Mixer mixer, Executor ui) {
        this.keyboard = keyboard;
        this.mouse = mouse;
        this.msg = msg;
        this.seeds = seeds;
        this.mixer = mixer;
        this.ui = ui;
        this.hud = new Hud(msg, painter.fighter());
        this.effects = new Effects(mixer, looks);
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.LEFT, settings.keys("left", KeyEvent.VK_LEFT, KeyEvent.VK_A))
                .bind(Action.RIGHT, settings.keys("right", KeyEvent.VK_RIGHT, KeyEvent.VK_D))
                .bind(Action.FIRE, settings.keys("fire", KeyEvent.VK_SPACE, KeyEvent.VK_CONTROL, KeyEvent.VK_UP))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.START, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        scenes.push(new TitleScene());
    }

    /** Starts the loop; the canvas draws the scenes' worlds and then their overlays. */
    public void start(GameCanvas canvas) {
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    void update(double seconds) {
        keyboard.poll();
        mouse.poll();
        scenes.update(seconds);
    }

    /** Enter, Space or a click: on to the next screen. */
    private boolean proceed() {
        return controls.wasPressed(Action.START) || mouse.wasPressed(MouseEvent.BUTTON1);
    }

    /** The world of all scenes, in logical coordinates. */
    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }

    /** Score and texts of all scenes, drawn after the world at the screen's full size. */
    public void renderOverlay(Graphics2D g, double alpha) {
        scenes.renderOverlay(g, alpha);
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void newGame() {
        run(() -> scenes.replace(new PlayScene()));
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof PlayScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public void onGameOver(Consumer<GameResult> listener) {
        onGameOver = listener;
    }

    /** On the loop thread when it runs; directly before the loop starts and in tests. */
    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    int sceneCount() {
        return scenes.size();
    }

    Effects effects() {
        return effects;
    }

    // ---- the scenes ------------------------------------------------------------------------------

    /** The computer plays, silently, game after game; Enter, Space or a click starts a game. */
    final class TitleScene implements Scene {
        private Game demo;
        private Pilot pilot;
        private double over;

        TitleScene() {
            effects.clear(true);
            startDemo();
        }

        private void startDemo() {
            demo = new Game(seeds.getAsLong());
            pilot = new Pilot(demo.seed());
            over = 0;
        }

        @Override
        public void update(double seconds) {
            if (proceed()) {
                scenes.replace(new PlayScene());
                return;
            }
            demo.update(seconds, pilot.steer(demo, seconds));
            effects.react(demo.events(), false);
            effects.update(seconds);
            if (demo.phase() == Game.Phase.OVER) {
                over += seconds;
                if (over > 3) startDemo();
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, demo, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("window.title"), 140, 72, true, Hud.GOLD);
            hud.text(g, msg.get("title.start"), 200, 18, false, Hud.CHALK);
            hud.text(g, msg.get("title.keys"), 268, 13, false, Hud.CHALK);
            hud.text(g, msg.get("title.mouse"), 290, 13, false, Hud.CHALK);
            hud.text(g, msg.get("title.screen"), 312, 13, false, Hud.CHALK);
        }

        Game demo() {
            return demo;
        }
    }

    // --8<-- [start:play]
    /**
     * A game. The keys move the fighter; once the mouse moves, the fighter follows the pointer instead,
     * until a key moves it again. A left click fires.
     */
    final class PlayScene implements Scene {
        private final Game game = new Game(seeds.getAsLong());
        private boolean mouseSteers;

        PlayScene() {
            effects.clear(true);
            effects.react(game.events(), true);    // the first stage's start: its fanfare
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            game.update(seconds, steering());
            effects.react(game.events(), true);
            effects.update(seconds);
            for (Event event : game.events()) {
                if (event instanceof Event.GameOver(GameResult result)) {
                    scenes.replace(new OverScene(game, result));
                    // The presenter lives on the Swing thread: hand the result over there.
                    Consumer<GameResult> listener = onGameOver;
                    ui.execute(() -> listener.accept(result));
                }
            }
        }

        private Steering steering() {
            int move = controls.axis(Action.LEFT, Action.RIGHT);
            if (move != 0) mouseSteers = false;
            else if (mouse.wasMoved()) mouseSteers = true;
            double target = mouseSteers && !Double.isNaN(mouse.x()) ? mouse.x() : Double.NaN;
            return new Steering(move, target, controls.wasPressed(Action.FIRE) || mouse.wasPressed(MouseEvent.BUTTON1));
        }
        // --8<-- [end:play]

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, game, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, game, effects);
            if (game.inIntro()) {
                hud.text(g, game.challenge() ? msg.get("play.challenge") : msg.format("play.stage", game.stage()),
                        190, 28, true, Hud.GOLD);
            }
            Event.ChallengeOver result = effects.challengeResult();
            if (result != null) {
                hud.text(g, msg.format("play.hits", result.hits(), result.of()), 190, 24, true, Hud.CYAN);
                if (result.bonus() > 0) {
                    hud.text(g, msg.format("play.perfect", result.bonus()), 222, 20, true, Hud.GOLD);
                }
            }
        }

        Game game() {
            return game;
        }

        boolean mouseSteers() {
            return mouseSteers;
        }
    }

    /** Drawn over the frozen game until P, Esc, Enter, Space or a click. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE) || proceed()) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("pause.title"), 200, 40, true, Hud.CHALK);
            hud.text(g, msg.get("pause.continue"), 245, 18, false, Hud.CHALK);
        }
    }

    /** The end: the swarm flies on, the score stays, Enter, Space or a click leads back to the title. */
    final class OverScene implements Scene {
        private final Game game;
        private final GameResult result;
        private double shown;

        OverScene(Game game, GameResult result) {
            this.game = game;
            this.result = result;
        }

        @Override
        public void update(double seconds) {
            game.update(seconds, Steering.NONE);
            effects.update(seconds);
            shown += seconds;
            // A short wait, so a shot meant for an insect does not skip the end.
            if (shown > 1.5 && proceed()) scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, game, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, game, effects);
            hud.veil(g);
            hud.text(g, msg.get("over.lost"), 190, 40, true, Hud.GOLD);
            hud.text(g, msg.format("over.score", result.score(), result.stage()), 232, 20, false, Hud.CHALK);
            hud.text(g, msg.get("over.continue"), 270, 16, false, Hud.CHALK);
        }
    }
}
