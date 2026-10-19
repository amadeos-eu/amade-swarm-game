package eu.amadeos.games.swarm.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.motion.FlightPath;
import eu.amadeos.games.swarm.bootstrap.SwarmModule;
import eu.amadeos.games.swarm.domain.Event;
import eu.amadeos.games.swarm.domain.Field;
import eu.amadeos.games.swarm.domain.Flight;
import eu.amadeos.games.swarm.domain.Game;
import eu.amadeos.games.swarm.domain.Stages;
import eu.amadeos.games.swarm.repository.SwarmSettings;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * The screenshots of chapter 24, drawn without a screen: the title, the paths of the first stage, a game
 * with divers, the sprite sheet. {@code ./gradlew screenshots -Pshots=<folder>}.
 */
public final class Screenshots {
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final SwarmGame game;

    private Screenshots() throws Exception {
        Messages msg = Messages.load(SwarmModule.class, SwarmModule.MESSAGES, Locale.ENGLISH);
        long[] seeds = {11};
        game = new SwarmGame(keyboard, new Mouse(), msg,
                new SwarmSettings(Files.createTempDirectory("swarm").resolve("swarm.properties")), () -> seeds[0]++,
                new SplittableRandom(11), new Mixer(), Runnable::run);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        Screenshots shots = new Screenshots();
        shots.steps(14 * 120);
        shots.save(new File(dir, "title.png"), true, null);
        // A game: the first stage flying in, with its paths drawn over it.
        shots.tap(KeyEvent.VK_ENTER);
        SwarmGame.PlayScene play = (SwarmGame.PlayScene) shots.game.top();
        shots.steps((int) ((Field.STAGE_INTRO + 4.6) * 120));
        Stages stages = Stages.STANDARD;
        shots.save(new File(dir, "paths.png"), false, List.of(stages.path("top-left"), stages.path("top-right"),
                stages.path("side-left")));
        // The demo in a later moment: the formation breathing, divers, shots and an explosion.
        shots.tap(KeyEvent.VK_ESCAPE);
        shots.game.showTitle();
        SwarmGame.TitleScene title = (SwarmGame.TitleScene) shots.game.top();
        for (int i = 0; i < 600 * 120; i++) {
            shots.steps(1);
            Game demo = title.demo();
            boolean hit = demo.events().stream().anyMatch(e -> e instanceof Event.InsectDestroyed);
            if (hit && demo.fighter() != null && demo.insects().count(Flight.class) >= 2 && demo.stage() >= 2
                    && demo.insectShots().size() >= 1 && demo.formation().breathing()) break;
        }
        shots.steps(3);
        shots.save(new File(dir, "play.png"), false, null);
        // The challenge stage of the demo.
        for (int i = 0; i < 600 * 120; i++) {
            shots.steps(1);
            Game demo = title.demo();
            if (demo.challenge() && !demo.inIntro() && demo.insects().count(Flight.class) >= 14) break;
        }
        shots.save(new File(dir, "challenge.png"), false, null);
        sheet(new File(dir, "sheet.png"));
        System.exit(0);
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(4);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (SwarmGame.STEP * 1e9);
            game.update(SwarmGame.STEP);
        }
    }

    /** Twice the logical size, as on a 1280 × 800 screen; the paths, if any, as thin lines. */
    private void save(File file, boolean overlay, List<FlightPath> paths) throws Exception {
        BufferedImage image = new BufferedImage(2 * (int) Field.WIDTH, 2 * (int) Field.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(2, 2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        game.render(g, 0);
        if (paths != null) {
            g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{3, 3}, 0));
            g.setColor(new Color(0x60A0FF));
            for (FlightPath path : paths) {
                Path2D.Double line = new Path2D.Double();
                line.moveTo(path.start().x(), path.start().y());
                for (double d = 2; d <= path.length(); d += 2) line.lineTo(path.at(d).x(), path.at(d).y());
                g.draw(line);
            }
        }
        if (overlay) {
            game.renderOverlay(g, 0);
        } else if (game.top() instanceof SwarmGame.PlayScene) {
            game.renderOverlay(g, 0);
        }
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    /** The sprite sheet, four times enlarged with hard edges, on dark grey. */
    private static void sheet(File file) throws Exception {
        BufferedImage sheet = ImageIO.read(Screenshots.class.getResource("/eu/amadeos/games/swarm/sprites.png"));
        int scale = 3;
        BufferedImage image = new BufferedImage(sheet.getWidth() * scale, sheet.getHeight() * scale,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0x181C24));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(sheet, 0, 0, image.getWidth(), image.getHeight(), null);
        g.setColor(new Color(0x303848));
        for (int x = 0; x <= image.getWidth(); x += 16 * scale) g.drawLine(x, 0, x, image.getHeight());
        for (int y = 0; y <= image.getHeight(); y += 16 * scale) g.drawLine(0, y, image.getWidth(), y);
        g.dispose();
        ImageIO.write(image, "png", file);
    }
}
