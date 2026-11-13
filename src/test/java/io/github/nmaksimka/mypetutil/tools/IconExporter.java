package io.github.nmaksimka.mypetutil.tools;

import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.PixelGrid;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Делает иконку программы myPetUtil.ico из спрайта пупса (размеры 16, 32, 48, 256).
 * Картинки внутри .ico хранятся как PNG — так умеет Windows начиная с Vista.
 */
public final class IconExporter {

    private static final int[] SIZES = {16, 32, 48, 64, 128, 256};

    private IconExporter() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "src/main/packaging/myPetUtil.ico");
        Files.createDirectories(out.getParent());
        PixelGrid grid = PetCharacter.CLAUDE.art().frame(Pose.of(Anim.IDLE, 0));

        List<byte[]> images = new ArrayList<>();
        for (int size : SIZES) {
            images.add(png(grid, size));
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        writeShort(data, 0);              // зарезервировано
        writeShort(data, 1);              // тип: иконка
        writeShort(data, SIZES.length);   // сколько картинок
        int offset = 6 + 16 * SIZES.length;
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            data.writeByte(size >= 256 ? 0 : size);
            data.writeByte(size >= 256 ? 0 : size);
            data.writeByte(0);            // палитры нет
            data.writeByte(0);
            writeShort(data, 1);          // плоскости
            writeShort(data, 32);         // бит на пиксель
            writeInt(data, images.get(i).length);
            writeInt(data, offset);
            offset += images.get(i).length;
        }
        for (byte[] image : images) {
            data.write(image);
        }
        Files.write(out, bytes.toByteArray());
        System.out.println("saved " + out.toAbsolutePath());
    }

    private static byte[] png(PixelGrid grid, int size) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int factor = size / grid.width();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, grid.get(x / factor, y / factor));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** В .ico числа записаны младшим байтом вперёд. */
    private static void writeShort(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >> 8) & 0xFF);
    }

    private static void writeInt(DataOutputStream out, int value) throws IOException {
        writeShort(out, value & 0xFFFF);
        writeShort(out, (value >> 16) & 0xFFFF);
    }
}
