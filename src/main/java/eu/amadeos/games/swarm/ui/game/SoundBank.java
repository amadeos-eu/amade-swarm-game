package eu.amadeos.games.swarm.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.random.SeededRandom;

/** The sound effects of Swarm, made once at start: arcade tones and filtered noise, no recordings. */
final class SoundBank {
    /** A short falling blip: the fighter's shot. */
    final Sound fire = Sound.tone(Wave.SQUARE, 1500, 500, 0.07, 0.1);
    final Sound insectFired = Sound.tone(Wave.TRIANGLE, 900, 600, 0.06, 0.12);
    /** The whistle of an insect diving: high, falling slowly. */
    final Sound dive = Sound.tone(Wave.TRIANGLE, 1800, 500, 0.7, 0.1);
    /** A boss changes its colour. */
    final Sound bossHit = Sound.tone(Wave.SQUARE, 300, 520, 0.12, 0.15);
    final Sound insectDestroyed = rumble(0.25, 0.85, 0.45).then(Sound.tone(Wave.SQUARE, 600, 200, 0.05, 0.05));
    final Sound bossDestroyed = rumble(0.4, 0.9, 0.55);
    final Sound fighterLost = rumble(1.2, 0.96, 0.7).then(Sound.tone(Wave.TRIANGLE, 400, 80, 0.5, 0.25));
    final Sound fighterReady = Sound.tone(Wave.TRIANGLE, 440, 880, 0.15, 0.2);
    final Sound extraLife = ping().then(silence(0.05)).then(ping()).then(silence(0.05)).then(ping());

    /**
     * Noise with its high tones taken away, as in Rocks: each sample moves only part of the way towards a
     * new random value. The larger {@code darkness}, 0 to 1, the deeper the bang.
     */
    static Sound rumble(double seconds, double darkness, double volume) {
        int n = (int) Math.round(seconds * Sound.RATE);
        SeededRandom random = new SeededRandom(n);
        double[] wave = new double[n];
        double value = 0;
        double loudest = 1e-9;
        for (int i = 0; i < n; i++) {
            value += (1 - darkness) * ((random.nextDouble() * 2 - 1) - value);
            wave[i] = value;
            loudest = Math.max(loudest, Math.abs(value));
        }
        short[] samples = new short[n];
        int attack = Sound.RATE / 400;
        for (int i = 0; i < n; i++) {
            double envelope = Math.min(1, i / (double) attack) * Math.exp(-4.0 * i / n) * (n - 1 - i) / n;
            samples[i] = (short) Math.round(wave[i] / loudest * envelope * volume * Short.MAX_VALUE);
        }
        return Sound.of(samples);
    }

    private static Sound ping() {
        return Sound.tone(Wave.SQUARE, 1760, 1760, 0.06, 0.12);
    }

    private static Sound silence(double seconds) {
        return Sound.of(new short[(int) Math.round(seconds * Sound.RATE)]);
    }
}
