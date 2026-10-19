package eu.amadeos.games.swarm.ui.game;

import eu.amadeos.gameengine.math.Angles;
import eu.amadeos.gameengine.pool.Pool;
import eu.amadeos.gameengine.sprite.Animation;
import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.gameengine.sprite.SpriteSheet;
import eu.amadeos.games.swarm.domain.Body;
import eu.amadeos.games.swarm.domain.Field;
import eu.amadeos.games.swarm.domain.Fighter;
import eu.amadeos.games.swarm.domain.Game;
import eu.amadeos.games.swarm.domain.Insect;
import eu.amadeos.games.swarm.domain.Shot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Draws the world in logical coordinates (640 × 400): the stars, the insects and the fighter from one
 * sprite sheet of pixel art, the shots, the explosions. Nothing is turned while drawing: an insect's
 * direction picks one of sixteen columns of the sheet, turned in advance.
 */
final class SwarmPainter {
    static final Color SHOT = new Color(0xFFF4C0);
    static final Color INSECT_SHOT = new Color(0xFF5060);
    /** The sheet's columns: sixteen directions, the first pointing right, then clockwise. */
    static final int DIRECTIONS = 16;
    /** Row 8: the fighter, then the explosions of an insect and of the fighter, five frames each. */
    static final int EXTRAS_ROW = 8;

    // --8<-- [start:sheet]
    private final BufferedImage image = Sprite.readImage(SwarmPainter.class, "/eu/amadeos/games/swarm/sprites.png");
    /** 16 × 16 pixels per cell, shown at 16 × 16 logical units, with hard edges: pixel art. */
    private final SpriteSheet sheet = SpriteSheet.of(image, 16, 9, 16, 16).hardEdges();
    /** The same picture, cells shown twice as large: the fighter's explosion. */
    private final SpriteSheet large = SpriteSheet.of(image, 16, 9, 32, 32).hardEdges();
    /** The wings beat for all insects together, as in the arcade: row 0 and row 1 of each insect. */
    private final Animation wings = new Animation(Animation.Mode.LOOP, 0.32, 0, 1);
    private final Animation insectBoom = new Animation(Animation.Mode.ONCE, 0.07, 1, 2, 3, 4, 5);
    private final Animation fighterBoom = new Animation(Animation.Mode.ONCE, 0.12, 6, 7, 8, 9, 10);
    // --8<-- [end:sheet]
    private final Rectangle2D.Double dot = new Rectangle2D.Double();
    private final Rectangle2D.Double space = new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT);

    // What drawInsect needs besides the insect, set once per frame: no lambda captures anything.
    private Graphics2D g;
    private double alpha;
    private int wingRow;

    // --8<-- [start:paint]
    /** @param alpha how far the time is between the last two update steps, 0 to 1 */
    void paint(Graphics2D g, Game game, Effects effects, double alpha) {
        g.setColor(Color.BLACK);
        g.fill(space);
        stars(g, effects, alpha);
        this.g = g;
        this.alpha = alpha;
        this.wingRow = wings.frame(effects.time());
        game.insects().forEach(Body.class, Insect.class, (entity, body, insect) -> drawInsect(body, insect));
        Fighter fighter = game.fighter();
        if (fighter != null) {
            double x = fighter.previousX() + (fighter.x() - fighter.previousX()) * alpha;
            sheet.frame(0, EXTRAS_ROW).draw(g, x - 8, Field.FIGHTER_Y - 8);
        }
        shots(g, game.shots(), SHOT, 1.5, 6);
        shots(g, game.insectShots(), INSECT_SHOT, 2, 4);
        explosions(g, effects);
        effects.particles().render(g, alpha);
        this.g = null;
    }

    /** The cell of an insect: its kind's two rows, the wing beat, and the column nearest its direction. */
    private void drawInsect(Body body, Insect insect) {
        double x = body.previous().x() + (body.position().x() - body.previous().x()) * alpha;
        double y = body.previous().y() + (body.position().y() - body.previous().y()) * alpha;
        double angle = body.previousAngle() + Angles.difference(body.previousAngle(), body.angle()) * alpha;
        sheet.frame(column(angle), firstRow(insect) + wingRow).draw(g, x - 8, y - 8);
    }

    /** Bees in rows 0 and 1, butterflies in 2 and 3, bosses in 4 and 5, hit bosses in 6 and 7. */
    static int firstRow(Insect insect) {
        return switch (insect.kind()) {
            case BEE -> 0;
            case BUTTERFLY -> 2;
            case BOSS -> insect.hits() > 1 ? 4 : 6;
        };
    }
    // --8<-- [end:paint]

    private void stars(Graphics2D g, Effects effects, double alpha) {
        double ahead = alpha / SwarmGame.UPDATES_PER_SECOND;
        for (int i = 0; i < Effects.STARS; i++) {
            // Each star blinks now and then, at a moment of its own.
            if ((effects.time() * 1.3 + i * 0.37) % 3 < 0.25) continue;
            g.setColor(effects.starColour[i]);
            dot.setRect(effects.starX[i], effects.starY[i] + effects.starSpeed[i] * ahead, 1.2, 1.2);
            g.fill(dot);
        }
    }

    private void shots(Graphics2D g, Pool<Shot> shots, Color colour, double width, double height) {
        g.setColor(colour);
        for (int i = 0; i < shots.size(); i++) {
            Shot shot = shots.get(i);
            double x = shot.previous().x() + (shot.position().x() - shot.previous().x()) * alpha;
            double y = shot.previous().y() + (shot.position().y() - shot.previous().y()) * alpha;
            dot.setRect(x - width / 2, y - height / 2, width, height);
            g.fill(dot);
        }
    }

    private void explosions(Graphics2D g, Effects effects) {
        for (int i = 0; i < Effects.EXPLOSIONS; i++) {
            double age = effects.time() - effects.boomTime[i];
            Animation boom = effects.boomBig[i] ? fighterBoom : insectBoom;
            if (age < 0 || boom.finished(age)) continue;
            if (effects.boomBig[i]) {
                large.frame(boom.frame(age), EXTRAS_ROW).draw(g, effects.boomX[i] - 16, effects.boomY[i] - 16);
            } else {
                sheet.frame(boom.frame(age), EXTRAS_ROW).draw(g, effects.boomX[i] - 8, effects.boomY[i] - 8);
            }
        }
    }

    /** The fighter's picture, for the fighters left in the overlay. */
    Sprite fighter() {
        return sheet.frame(0, EXTRAS_ROW);
    }

    /** The column nearest to a direction: 0 for right, 4 for down, 12 for up. */
    static int column(double angle) {
        return Math.floorMod(Math.round(angle / (2 * Math.PI / DIRECTIONS)), DIRECTIONS);
    }
}
