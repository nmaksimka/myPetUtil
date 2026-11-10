package io.github.nmaksimka.mypetutil.tools;

import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.FoodType;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.PixelGrid;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Выгружает все кадры всех персонажей в одну картинку.
 * Нужен, чтобы посмотреть спрайты глазами: gradlew exportSprites.
 */
public final class SpriteSheetExporter {

    private static final int SCALE = 8;
    private static final int CELL = 16 * SCALE + 8;

    private SpriteSheetExporter() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "build/sprites");
        Files.createDirectories(out);

        int columns = 0;
        for (Anim anim : Anim.values()) {
            columns += anim.frames();
        }
        columns += 1; // кадр с морганием
        PetCharacter[] characters = PetCharacter.values();
        BufferedImage sheet = new BufferedImage(columns * CELL, (characters.length + 1) * CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0xE9E6E1));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());

        for (int row = 0; row < characters.length; row++) {
            int col = 0;
            for (Anim anim : Anim.values()) {
                for (int f = 0; f < anim.frames(); f++) {
                    paint(sheet, characters[row].art().frame(Pose.of(anim, f)), col++ * CELL + 4, row * CELL + 4, SCALE);
                }
            }
            paint(sheet, characters[row].art().frame(new Pose(Anim.IDLE, 0, true)), col * CELL + 4, row * CELL + 4, SCALE);
        }
        int col = 0;
        for (FoodType food : FoodType.values()) {
            paint(sheet, food.grid(), col++ * CELL + 4, characters.length * CELL + 4, SCALE);
        }
        g.dispose();
        ImageIO.write(sheet, "png", out.resolve("sheet.png").toFile());
        System.out.println("saved " + out.resolve("sheet.png").toAbsolutePath());
    }

    static void paint(BufferedImage target, PixelGrid grid, int ox, int oy, int scale) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                int argb = grid.get(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        target.setRGB(ox + x * scale + dx, oy + y * scale + dy, argb);
                    }
                }
            }
        }
    }
}
