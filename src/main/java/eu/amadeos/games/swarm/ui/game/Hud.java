package eu.amadeos.games.swarm.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.games.swarm.domain.Field;
import eu.amadeos.games.swarm.domain.Game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The overlay: score and stage at the top, the fighters left at the bottom, the points of fallen flying
 * insects, and the texts of the title, pause and end screens. The canvas draws it after the world, at the
 * full size of the screen, so the letters are sharp in every render mode.
 */
final class Hud {
    static final Color CHALK = Color.WHITE;
    static final Color GOLD = new Color(0xF0C040);
    static final Color CYAN = new Color(0x8FC8FF);
    /** A veil over the field behind texts. */
    static final Color VEIL = new Color(0, 0, 0, 150);
    /** How long the points of a fallen insect stay. */
    static final double POINTS_SECONDS = 0.8;

    private final Messages msg;
    private final Sprite fighter;
    private final Font score = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(20f);
    private final Font small = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(13f);
    private final Font points = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(11f);
    private final Map<Integer, Font> fonts = new HashMap<>();
    private final Rectangle2D.Double veil = new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT);

    Hud(Messages msg, Sprite fighter) {
        this.msg = msg;
        this.fighter = fighter;
    }

    // --8<-- [start:hud]
    /** The score at the top left, the stage at the top right, a small fighter for each one left below. */
    void status(Graphics2D g, Game game, Effects effects) {
        g.setColor(CHALK);
        g.setFont(score);
        g.drawString(msg.format("hud.score", game.score()), 14f, 26f);
        g.setFont(small);
        FontMetrics fm = g.getFontMetrics();
        String stage = msg.format("hud.stage", game.stage());
        g.drawString(stage, (float) (Field.WIDTH - 14 - fm.stringWidth(stage)), 24f);
        int waiting = game.lives() - (game.fighter() != null ? 1 : 0);
        for (int i = 0; i < waiting; i++) fighter.draw(g, 8 + i * 18, Field.HEIGHT - 18);
        g.setFont(points);
        g.setColor(CYAN);
        for (int i = 0; i < Effects.EXPLOSIONS; i++) {
            double age = effects.time() - effects.boomTime[i];
            if (effects.boomPoints[i] == 0 || age < 0 || age > POINTS_SECONDS) continue;
            String text = Integer.toString(effects.boomPoints[i]);
            g.drawString(text, (float) (effects.boomX[i] - g.getFontMetrics().stringWidth(text) / 2.0),
                    (float) (effects.boomY[i] + 4 - age * 12));
        }
    }
    // --8<-- [end:hud]

    /** Darkens the field so text over it stays readable. */
    void veil(Graphics2D g) {
        Object smoothing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(VEIL);
        g.fill(veil);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, smoothing);
    }

    /** A line of text centred horizontally, its baseline at {@code y}. */
    void text(Graphics2D g, String line, double y, int size, boolean bold, Color colour) {
        g.setFont(fonts.computeIfAbsent(2 * size + (bold ? 1 : 0),
                key -> new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size)));
        g.setColor(colour);
        g.drawString(line, (float) (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2, (float) y);
    }
}
