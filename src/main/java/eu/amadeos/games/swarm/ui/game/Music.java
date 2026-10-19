package eu.amadeos.games.swarm.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.audio.Tune;

/**
 * The music of Swarm, written in MML and made into sounds once at start: a fanfare when a game begins, a
 * tune that loops through a challenge stage, a short tune for a cleared stage and one for the end. Melody
 * and bass are separate voices, mixed into one sound; the mixer plays it on its music voice.
 */
final class Music {
    // --8<-- [start:music]
    /** A game begins: up the C major chord, then D minor, E minor, and home. */
    final Sound start = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.12, "t160 o5 l16 c e g >c< g e c e  d f a >d< a f d f"
                    + "| e g b >e< b g e g  c4 r4"),
            Tune.voice(Wave.TRIANGLE, 0.25, "t160 o3 l4 c c d d | e e c r"));

    /** A challenge stage: two bars over a walking bass, again and again. */
    final Sound challenge = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.08, "t140 o5 l8 e g a g e d c d | e g a >c< a g e r"
                    + "| d f g f d c < b > c | d f g b g f d r"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t140 o3 l8 a >c e c< a >c e c< | a >c e c< a >c e c<"
                    + "| g b >d< b g b >d< b | g b >d< b g b >d< b"));
    // --8<-- [end:music]

    /** A stage cleared: three quick notes up. */
    final Sound cleared = Tune.voice(Wave.SQUARE, 0.12, "t180 o5 l16 c e g >c8");

    /** The end: down, slowly. */
    final Sound over = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.1, "t90 o5 l8 g f e d c4. r8"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t90 o3 l4 c < g > c2"));
}
