import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Draws the sprite sheet of Swarm, {@code sprites.png}: pixel art of 16 × 16 pixels per cell, written below
 * as text, one letter per pixel. Each insect comes in two wing positions and sixteen directions; the
 * directions are turned here, once, as the artists of the arcade turned theirs by hand, so the game only
 * picks a cell and never turns a picture.
 *
 * <pre>
 * java tools/MakeSprites.java src/main/resources/eu/amadeos/games/swarm/sprites.png
 * </pre>
 *
 * Rows of the sheet: 0-1 bee, 2-3 butterfly, 4-5 boss, 6-7 boss after the first hit (each two wing
 * positions, the 16 columns the directions, column 0 pointing right, then clockwise); row 8: the fighter,
 * then five frames of an insect's explosion, then five of the fighter's. The file is part of the
 * repository; run this again only to change the pictures.
 */
public class MakeSprites {
    static final int CELL = 16;
    static final int DIRECTIONS = 16;

    static final Map<Character, Integer> PALETTE = Map.ofEntries(
            Map.entry('.', 0x00000000),
            Map.entry('Y', 0xFFFFD83A),     // yellow
            Map.entry('O', 0xFFFF9A2E),     // orange
            Map.entry('R', 0xFFE8303A),     // red
            Map.entry('P', 0xFFFF7088),     // pink
            Map.entry('B', 0xFF3A6CFF),     // blue
            Map.entry('L', 0xFF8FC8FF),     // light blue
            Map.entry('G', 0xFF2FC85A),     // green
            Map.entry('D', 0xFF1E7A3C),     // dark green
            Map.entry('V', 0xFF9A5CFF),     // violet
            Map.entry('W', 0xFFF4F4FF),     // white
            Map.entry('S', 0xFFA8B0C8),     // silver
            Map.entry('K', 0xFF202840));    // dark

    // All pictures point up, the way the insects fly into the screen from the top... and the fighter flies.
    static final String[] BEE_A = {
            "................",
            ".......RR.......",
            "......RYYR......",
            "..L...YKKY...L..",
            ".LLL..YYYY..LLL.",
            "LLLLL.YBBY.LLLLL",
            "LLLLLYYYYYYLLLLL",
            ".LLLLYBBBBYLLLL.",
            "..LL.YYYYYY.LL..",
            ".....YBBBBY.....",
            ".....YYYYYY.....",
            "......YBBY......",
            "......YYYY......",
            ".......YY.......",
            ".......RR.......",
            "................"};
    static final String[] BEE_B = {
            "................",
            ".......RR.......",
            "......RYYR......",
            "......YKKY......",
            "....L.YYYY.L....",
            "...LL.YBBY.LL...",
            "..LLLYYYYYYLLL..",
            ".LLLLYBBBBYLLLL.",
            ".LLL.YYYYYY.LLL.",
            "..L..YBBBBY..L..",
            ".....YYYYYY.....",
            "......YBBY......",
            "......YYYY......",
            ".......YY.......",
            ".......RR.......",
            "................"};
    static final String[] BUTTERFLY_A = {
            "................",
            "....W......W....",
            ".....W....W.....",
            "......WRRW......",
            ".BB...RKKR...BB.",
            "BRRB..RRRR..BRRB",
            "BRRRB.RWWR.BRRRB",
            "BRWRRBRRRRBRRWRB",
            "BRRRRBRWWRBRRRRB",
            ".BRRB.RRRR.BRRB.",
            "..BB..RWWR..BB..",
            "......RRRR......",
            ".......RR.......",
            ".......RR.......",
            "................",
            "................"};
    static final String[] BUTTERFLY_B = {
            "................",
            "....W......W....",
            ".....W....W.....",
            "......WRRW......",
            "......RKKR......",
            "...BB.RRRR.BB...",
            "..BRRBRWWRBRRB..",
            ".BRWRRRRRRRRWRB.",
            ".BRRRBRWWRBRRRB.",
            "..BRB.RRRR.BRB..",
            "...B..RWWR..B...",
            "......RRRR......",
            ".......RR.......",
            ".......RR.......",
            "................",
            "................"};
    static final String[] BOSS_A = {
            "...O........O...",
            "....O..GG..O....",
            ".....OGGGGO.....",
            "....GGKGGKGG....",
            "...GGGGGGGGGG...",
            "VV.GGDGGGGDGG.VV",
            "VVVGGGGDDGGGGVVV",
            "VVVVGGDGGDGGVVVV",
            "VVVVYGGGGGGYVVVV",
            ".VVVYYGDDGYYVVV.",
            "..VV.YYGGYY.VV..",
            "......YGGY......",
            ".......GG.......",
            "......G..G......",
            ".....G....G.....",
            "................"};
    static final String[] BOSS_B = {
            "...O........O...",
            "....O..GG..O....",
            ".....OGGGGO.....",
            "....GGKGGKGG....",
            "...GGGGGGGGGG...",
            "...GGDGGGGDGG...",
            "..VGGGGDDGGGGV..",
            ".VVVGGDGGDGGVVV.",
            "VVVVYGGGGGGYVVVV",
            "VVVVYYGDDGYYVVVV",
            ".VV..YYGGYY..VV.",
            "......YGGY......",
            ".......GG.......",
            "......G..G......",
            ".....G....G.....",
            "................"};
    static final String[] FIGHTER = {
            ".......WW.......",
            ".......WW.......",
            ".......WW.......",
            "......WWWW......",
            "......WRRW......",
            "...R..WWWW..R...",
            "...W.WWBBWW.W...",
            "...WWWWBBWWWW...",
            ".R.WWWWWWWWWW.R.",
            ".WWWWWWWWWWWWWW.",
            ".WWWRWWWWWWRWWW.",
            ".WW.RRWWWWRR.WW.",
            ".W....WWWW....W.",
            ".......RR.......",
            "................",
            "................"};

    public static void main(String[] args) throws IOException {
        BufferedImage sheet = new BufferedImage(DIRECTIONS * CELL, 9 * CELL, BufferedImage.TYPE_INT_ARGB);
        String[][] insects = {BEE_A, BEE_B, BUTTERFLY_A, BUTTERFLY_B, BOSS_A, BOSS_B,
                recolour(BOSS_A, "GDY", "LBW"), recolour(BOSS_B, "GDY", "LBW")};
        for (int row = 0; row < insects.length; row++) {
            for (int d = 0; d < DIRECTIONS; d++) {
                // Column d points at d × 22.5°; the pictures point up, at -90°.
                double turn = d * 2 * Math.PI / DIRECTIONS + Math.PI / 2;
                put(sheet, d, row, turned(insects[row], turn));
            }
        }
        put(sheet, 0, 8, pixels(FIGHTER));
        for (int f = 0; f < 5; f++) {
            put(sheet, 1 + f, 8, explosion(f, new int[]{0xFFFFFFFF, 0xFFFFD83A, 0xFFFF9A2E, 0xFFE8303A}, 41));
            put(sheet, 6 + f, 8, explosion(f, new int[]{0xFFFFFFFF, 0xFF8FC8FF, 0xFFE8303A, 0xFFFFD83A}, 7));
        }
        File file = new File(args[0]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", file);
    }

    static String[] recolour(String[] art, String from, String to) {
        String[] out = new String[art.length];
        for (int i = 0; i < art.length; i++) {
            StringBuilder line = new StringBuilder(art[i]);
            for (int x = 0; x < line.length(); x++) {
                int k = from.indexOf(line.charAt(x));
                if (k >= 0) line.setCharAt(x, to.charAt(k));
            }
            out[i] = line.toString();
        }
        return out;
    }

    static int[][] pixels(String[] art) {
        int[][] p = new int[CELL][CELL];
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) p[y][x] = PALETTE.get(art[y].charAt(x));
        }
        return p;
    }

    /** The picture turned by {@code angle} around its centre: each new pixel takes the old pixel it came from. */
    static int[][] turned(String[] art, double angle) {
        int[][] source = pixels(art);
        int[][] p = new int[CELL][CELL];
        double cos = Math.cos(-angle);
        double sin = Math.sin(-angle);
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                double dx = x + 0.5 - CELL / 2.0;
                double dy = y + 0.5 - CELL / 2.0;
                int sx = (int) Math.floor(dx * cos - dy * sin + CELL / 2.0);
                int sy = (int) Math.floor(dx * sin + dy * cos + CELL / 2.0);
                if (sx >= 0 && sy >= 0 && sx < CELL && sy < CELL) p[y][x] = source[sy][sx];
            }
        }
        return p;
    }

    /** Frame {@code f} of five: a ball of fire that grows, breaks into sparks and fades. */
    static int[][] explosion(int f, int[] colours, long seed) {
        int[][] p = new int[CELL][CELL];
        java.util.Random random = new java.util.Random(seed + f);
        double outer = 2.5 + f * 1.4;
        double inner = f < 2 ? 0 : (f - 1) * 1.6;
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                double r = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
                if (r > outer || r < inner) continue;
                double density = f < 2 ? 1 : 1.0 - f * 0.17;
                if (random.nextDouble() > density) continue;
                int c = (int) Math.min(colours.length - 1, (r / outer) * colours.length * 0.8 + f * 0.5);
                p[y][x] = colours[c];
            }
        }
        return p;
    }

    static void put(BufferedImage sheet, int column, int row, int[][] p) {
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) sheet.setRGB(column * CELL + x, row * CELL + y, p[y][x]);
        }
    }
}
