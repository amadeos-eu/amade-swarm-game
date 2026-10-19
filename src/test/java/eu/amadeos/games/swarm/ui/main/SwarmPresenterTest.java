package eu.amadeos.games.swarm.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.swarm.bootstrap.SwarmModule;
import eu.amadeos.games.swarm.domain.GameResult;
import eu.amadeos.games.swarm.repository.SwarmSettings;
import eu.amadeos.games.swarm.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwarmPresenterTest {
    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private Consumer<GameResult> gameOver;
    private SwarmPresenter presenter;
    private HighScores scores;
    private SwarmSettings settings;

    private final SwarmView view = new SwarmView() {
        @Override
        public void showOptions(boolean sound, RenderMode mode) {
            log.add("options " + sound + " " + mode);
        }

        @Override
        public void useRenderMode(RenderMode mode) {
            log.add("mode " + mode);
        }

        @Override
        public Optional<String> askName(String message) {
            log.add("ask " + message);
            return Optional.of("Ada");
        }

        @Override
        public void showScores(int highlight) {
            log.add("scores " + highlight);
        }

        @Override
        public void showAbout() {
        }
    };

    private final Arena arena = new Arena() {
        @Override
        public void showTitle() {
            log.add("title");
        }

        @Override
        public void newGame() {
            log.add("new");
        }

        @Override
        public void pause() {
            log.add("pause");
        }

        @Override
        public void setSound(boolean on) {
            log.add("sound " + on);
        }

        @Override
        public void onGameOver(Consumer<GameResult> listener) {
            gameOver = listener;
        }
    };

    @BeforeEach
    void create() {
        Messages msg = Messages.load(SwarmModule.class, SwarmModule.MESSAGES, Locale.ENGLISH);
        settings = new SwarmSettings(dir.resolve("swarm.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.HIGHER_WINS);
        presenter = new SwarmPresenter(view, arena, msg, settings, scores, () -> Instant.EPOCH);
        presenter.start();
    }

    @Test
    void theSettingsAreAppliedAtStartAndKept() {
        assertEquals(List.of("options true DIRECT", "mode DIRECT", "sound true", "title"), log);
        log.clear();
        presenter.setRenderMode(RenderMode.PIXEL);
        presenter.setSound(false);
        assertEquals(List.of("mode PIXEL", "sound false"), log);
        assertEquals(RenderMode.PIXEL, settings.renderMode());
    }

    @Test
    void aGoodScoreEntersTheTable() {
        log.clear();
        gameOver.accept(new GameResult(12340, 4));
        assertEquals(List.of("ask 12,340 points, stage 4.", "scores 0"), log);
        assertEquals("Ada", scores.list(SwarmPresenter.TABLE).getFirst().name());
    }

    @Test
    void noPointsNoEntry() {
        log.clear();
        gameOver.accept(new GameResult(0, 1));
        assertEquals(List.of(), log);
    }

    @Test
    void losingTheFocusPausesTheGame() {
        log.clear();
        presenter.focusLost();
        presenter.newGame();
        assertEquals(List.of("pause", "new"), log);
    }
}
