import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Swarm from the game's own sprite sheet: a butterfly diving down at the
 * fighter, which shoots back, in dark space. Pixel art, enlarged with hard edges, as a PNG of the given size.
 *
 * <pre>
 * java tools/MakeIcon.java 48 src/main/resources/icons/amade-swarm-game.png
 * java tools/MakeIcon.java 256 build/icon-256.png
 * python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-swarm-game.ico',
 *     sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
 * </pre>
 *
 * The files are part of the repository; run this again only to change the icon.
 */
public class MakeIcon {
    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        BufferedImage sheet = ImageIO.read(new File("src/main/resources/eu/amadeos/games/swarm/sprites.png"));
        ImageIO.write(icon(size, sheet), "png", file);
    }

    static BufferedImage icon(int size, BufferedImage sheet) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(size / 48.0, size / 48.0);
        RoundRectangle2D.Double space = new RoundRectangle2D.Double(2, 2, 44, 44, 10, 10);
        g.setPaint(new GradientPaint(0, 2, new Color(0x1A2440), 0, 46, new Color(0x05070C)));
        g.fill(space);
        g.setColor(new Color(0x3A4A70));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(space);
        g.setColor(new Color(0xC0C8E0));
        for (double[] star : new double[][]{{9, 9}, {38, 36}, {8, 30}, {40, 8}}) {
            g.fill(new Rectangle2D.Double(star[0], star[1], 1.2, 1.2));
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // The butterfly, row 2, column 5: diving down and a little to the left.
        g.drawImage(sheet.getSubimage(5 * 16, 2 * 16, 16, 16), 8, 4, 24, 24, null);
        // The fighter, row 8, column 0, and its shot.
        g.drawImage(sheet.getSubimage(0, 8 * 16, 16, 16), 22, 28, 18, 18, null);
        g.setColor(new Color(0xFFF4C0));
        g.fill(new Rectangle2D.Double(30.2, 22, 1.6, 4));
        g.dispose();
        return image;
    }
}
