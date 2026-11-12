package io.github.nmaksimka.mypetutil.ui;

import javafx.scene.Node;

/** Одна вкладка окна настроек. */
public interface Page {

    String title();

    Node view();

    /** Вкладку открыли — обновить данные. */
    default void onShow() {
    }

    /** Вызывается каждый кадр, пока вкладка на экране. now — секунды. */
    default void tick(double now) {
    }
}
