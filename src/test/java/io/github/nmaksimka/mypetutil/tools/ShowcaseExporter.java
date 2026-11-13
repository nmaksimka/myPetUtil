package io.github.nmaksimka.mypetutil.tools;

import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.Emotes;
import io.github.nmaksimka.mypetutil.sprite.FoodType;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.PixelGrid;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Рисует картинку для README: макет рабочего стола с окном и панелью задач,
 * на которых стоят питомцы. Настоящий экран не фотографируется.
 */
public final class ShowcaseExporter {

    private static final int W = 1200;
    private static final int H = 420;
    private static final int P = 4;

    private ShowcaseExporter() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/showcase.png");
        Files.createDirectories(out.getParent());
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // обои
        g.setColor(new Color(0x2E3A4F));
        g.fillRect(0, 0, W, H);

        // окно браузера
        int wx = 140;
        int wy = 40;
        int ww = 620;
        int wh = 230;
        g.setColor(new Color(0xF6F7F9));
        g.fillRoundRect(wx, wy, ww, wh, 10, 10);
        g.setColor(new Color(0xE2E5EA));
        g.fillRect(wx, wy + 34, ww, 1);
        g.setColor(new Color(0xC9CED6));
        g.fillOval(wx + 14, wy + 12, 10, 10);
        g.fillOval(wx + 30, wy + 12, 10, 10);
        g.fillOval(wx + 46, wy + 12, 10, 10);
        g.setColor(new Color(0xE2E5EA));
        g.fillRoundRect(wx + 30, wy + 60, 300, 14, 6, 6);
        g.fillRoundRect(wx + 30, wy + 86, 420, 10, 6, 6);
        g.fillRoundRect(wx + 30, wy + 104, 380, 10, 6, 6);
        g.fillRoundRect(wx + 30, wy + 122, 400, 10, 6, 6);

        // панель задач
        int taskbarY = H - 48;
        g.setColor(new Color(0x1C2230));
        g.fillRect(0, taskbarY, W, 48);
        g.setColor(new Color(0x3A4458));
        for (int i = 0; i < 6; i++) {
            g.fillRoundRect(W / 2 - 120 + i * 42, taskbarY + 10, 28, 28, 6, 6);
        }

        int floor = taskbarY;
        int windowFloor = wy + wh;

        // на окне
        pet(image, PetCharacter.STEVE, Pose.of(Anim.WALK, 1), wx + 380, windowFloor, false);
        pet(image, PetCharacter.CLAUDE, Pose.of(Anim.HOVER, 0), wx + 480, windowFloor, true);
        grid(image, Emotes.HEART, wx + 480 + 8 * P - 14, windowFloor - 16 * P - 24, P);

        // на панели задач
        pet(image, PetCharacter.GHOST, Pose.of(Anim.IDLE, 1), 60, floor, false);
        pet(image, PetCharacter.SONIC, Pose.of(Anim.WALK, 3), 230, floor, false);
        pet(image, PetCharacter.MINION, Pose.of(Anim.EAT, 0), 840, floor, true);
        grid(image, FoodType.BANANA.grid(), 840 - 34, floor - FoodType.SIZE * P + 2, P);
        pet(image, PetCharacter.SAKURA, Pose.of(Anim.SAD, 1), 1010, floor, false);
        grid(image, FoodType.WATER.grid(), 1100, floor - FoodType.SIZE * P - 120, P);

        g.dispose();
        ImageIO.write(image, "png", out.toFile());
        System.out.println("saved " + out.toAbsolutePath());
    }

    private static void pet(BufferedImage image, PetCharacter character, Pose pose, int x, int floorY, boolean mirror) {
        PixelGrid frame = character.art().frame(pose);
        int size = 16 * P;
        int top = floorY - size;
        for (int y = 0; y < 16; y++) {
            for (int gx = 0; gx < 16; gx++) {
                int argb = frame.get(mirror ? 15 - gx : gx, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                fill(image, x + gx * P, top + y * P, P, argb);
            }
        }
    }

    private static void grid(BufferedImage image, PixelGrid grid, int x, int y, int scale) {
        for (int gy = 0; gy < grid.height(); gy++) {
            for (int gx = 0; gx < grid.width(); gx++) {
                int argb = grid.get(gx, gy);
                if ((argb >>> 24) != 0) {
                    fill(image, x + gx * scale, y + gy * scale, scale, argb);
                }
            }
        }
    }

    private static void fill(BufferedImage image, int x, int y, int size, int argb) {
        for (int dy = 0; dy < size; dy++) {
            for (int dx = 0; dx < size; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= 0 && py >= 0 && px < image.getWidth() && py < image.getHeight()) {
                    image.setRGB(px, py, argb);
                }
            }
        }
    }
}
