package io.github.nmaksimka.mypetutil.world;

/**
 * Горизонтальный отрезок, по которому можно ходить.
 *
 * @param owner   кому принадлежит пол: "screen:0" или "win:12345". У одного окна может быть
 *                несколько отрезков, если его низ частично закрыт другими окнами.
 * @param minX    левый край
 * @param maxX    правый край
 * @param y       высота пола (ноги питомца стоят на этой линии)
 * @param anchorX левый край окна-владельца, по нему видно, что окно сдвинули
 * @param screen  индекс монитора, на котором лежит пол
 */
public record Floor(String owner, FloorKind kind, double minX, double maxX, double y, double anchorX, int screen) {

    public double width() {
        return maxX - minX;
    }

    public boolean containsX(double x) {
        return x >= minX && x <= maxX;
    }
}
