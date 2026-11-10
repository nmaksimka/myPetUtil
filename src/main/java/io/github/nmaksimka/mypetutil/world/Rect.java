package io.github.nmaksimka.mypetutil.world;

/** Прямоугольник в экранных координатах JavaFX (логические точки, не физические пиксели). */
public record Rect(double minX, double minY, double maxX, double maxY) {

    public static Rect ofSize(double x, double y, double width, double height) {
        return new Rect(x, y, x + width, y + height);
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }

    public double centerX() {
        return (minX + maxX) / 2;
    }

    public double centerY() {
        return (minY + maxY) / 2;
    }

    public boolean contains(double x, double y) {
        return x >= minX && x < maxX && y >= minY && y < maxY;
    }

    public boolean intersects(Rect other) {
        return minX < other.maxX && other.minX < maxX && minY < other.maxY && other.minY < maxY;
    }
}
