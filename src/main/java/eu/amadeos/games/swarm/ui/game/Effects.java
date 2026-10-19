package eu.amadeos.games.swarm.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.fx.Particles;
import eu.amadeos.gameengine.math.Vector;
import eu.amadeos.games.swarm.domain.Event;
import eu.amadeos.games.swarm.domain.Field;
import eu.amadeos.games.swarm.domain.Kind;

import java.awt.Color;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * What the events of a step look and sound like: explosions from the sprite sheet, sparks, points that
 * rise where a flying insect fell, the stars that stream past, the sounds and the music. The rules know
 * nothing of this. Chance for the looks comes from a random generator of its own, never from the rules'.
 */
final class Effects {
    static final Color[] PALETTE = {new Color(0xFFD83A), Color.WHITE, new Color(0xFF9A2E), new Color(0x8FC8FF),
            new Color(0xE8303A)};
    private static final int YELLOW = 0;
    private static final int WHITE = 1;
    private static final int BLUE = 3;
    static final int STARS = 90;
    /** Most explosions and point labels at once; a new one replaces the oldest. */
    static final int EXPLOSIONS = 24;

    private final Particles particles = new Particles(800, PALETTE);
    private final SoundBank sounds = new SoundBank();
    private final Music music = new Music();
    private final Mixer mixer;
    private final RandomGenerator random;
    /** Seconds since the effects started: the clock of the wings' flapping and the stars' blinking. */
    private double time;
    // The stars, in arrays: x, y, speed, colour.
    final double[] starX = new double[STARS];
    final double[] starY = new double[STARS];
    final double[] starSpeed = new double[STARS];
    final Color[] starColour = new Color[STARS];
    // Explosions and point labels, in a ring: where, when, how big, and the points to show (0 for none).
    final double[] boomX = new double[EXPLOSIONS];
    final double[] boomY = new double[EXPLOSIONS];
    final double[] boomTime = new double[EXPLOSIONS];
    final boolean[] boomBig = new boolean[EXPLOSIONS];
    final int[] boomPoints = new int[EXPLOSIONS];
    private int nextBoom;
    /** The result of the last challenge stage, for the overlay; null outside a challenge's end. */
    private Event.ChallengeOver challengeResult;

    Effects(Mixer mixer, RandomGenerator random) {
        this.mixer = mixer;
        this.random = random;
        for (int i = 0; i < STARS; i++) {
            starX[i] = random.nextDouble() * Field.WIDTH;
            starY[i] = random.nextDouble() * Field.HEIGHT;
            starSpeed[i] = 20 + random.nextDouble() * 60;
            int light = 70 + random.nextInt(150);
            starColour[i] = new Color(light, light, Math.min(255, light + 40));
        }
        java.util.Arrays.fill(boomTime, Double.NEGATIVE_INFINITY);
    }

    Particles particles() {
        return particles;
    }

    double time() {
        return time;
    }

    Event.ChallengeOver challengeResult() {
        return challengeResult;
    }

    /** A new game or the title: no sparks, no explosions, no music from before. */
    void clear(boolean stopMusic) {
        particles.clear();
        java.util.Arrays.fill(boomTime, Double.NEGATIVE_INFINITY);
        challengeResult = null;
        if (stopMusic) mixer.stopMusic();
    }

    /** The stars stream down, faster ones nearer; sparks fly. */
    void update(double seconds) {
        time += seconds;
        particles.update(seconds, 0);
        for (int i = 0; i < STARS; i++) {
            starY[i] += starSpeed[i] * seconds;
            if (starY[i] > Field.HEIGHT) {
                starY[i] -= Field.HEIGHT;
                starX[i] = random.nextDouble() * Field.WIDTH;
            }
        }
    }

    // --8<-- [start:react]
    /** Shows and, if {@code audible}, plays what happened in the last step; the music too. */
    void react(List<Event> events, boolean audible) {
        for (Event event : events) {
            switch (event) {
                case Event.Fired fired -> {
                    if (audible) mixer.play(sounds.fire);
                }
                case Event.InsectFired fired -> {
                    if (audible) mixer.play(sounds.insectFired);
                }
                case Event.Dived dived -> {
                    if (audible) mixer.play(sounds.dive);
                }
                case Event.InsectHit(Kind kind, Vector at) -> {
                    burst(at, BLUE, 12, 80);
                    if (audible) mixer.play(sounds.bossHit);
                }
                case Event.InsectDestroyed(Kind kind, Vector at, int points) -> {
                    explode(at, false, points >= 150 ? points : 0);
                    burst(at, YELLOW, 14, 90);
                    if (audible) mixer.play(kind == Kind.BOSS ? sounds.bossDestroyed : sounds.insectDestroyed);
                }
                case Event.FighterLost(Vector at) -> {
                    explode(at, true, 0);
                    burst(at, WHITE, 40, 120);
                    if (audible) mixer.play(sounds.fighterLost);
                }
                case Event.FighterReady ready -> {
                    if (audible) mixer.play(sounds.fighterReady);
                }
                case Event.StageStarted(int stage, boolean challenge) -> {
                    challengeResult = null;
                    if (audible && challenge) mixer.playMusic(music.challenge, true);
                    else if (audible && stage == 1) mixer.playMusic(music.start, false);
                }
                case Event.StageCleared cleared -> {
                    if (audible) mixer.playMusic(music.cleared, false);
                }
                case Event.ChallengeOver over -> challengeResult = over;
                case Event.ExtraLife life -> {
                    if (audible) mixer.play(sounds.extraLife);
                }
                case Event.GameOver over -> {
                    if (audible) mixer.playMusic(music.over, false);
                }
            }
        }
    }
    // --8<-- [end:react]

    private void explode(Vector at, boolean big, int points) {
        boomX[nextBoom] = at.x();
        boomY[nextBoom] = at.y();
        boomTime[nextBoom] = time;
        boomBig[nextBoom] = big;
        boomPoints[nextBoom] = points;
        nextBoom = (nextBoom + 1) % EXPLOSIONS;
    }

    /** {@code count} sparks fly out of {@code at} in all directions. */
    private void burst(Vector at, int colour, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double v = speed * (0.2 + random.nextDouble() * 0.8);
            particles.spawn(at.x(), at.y(), Math.cos(angle) * v, Math.sin(angle) * v, 0.3 + random.nextDouble() * 0.4,
                    1 + random.nextDouble(), random.nextDouble() < 0.2 ? WHITE : colour);
        }
    }
}
