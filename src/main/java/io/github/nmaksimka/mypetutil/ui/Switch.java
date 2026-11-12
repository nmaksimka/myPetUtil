package io.github.nmaksimka.mypetutil.ui;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Переключатель вкл/выкл. В стандартном JavaFX такого нет, только CheckBox. */
public final class Switch extends StackPane {

    private static final PseudoClass ON = PseudoClass.getPseudoClass("on");
    private static final double WIDTH = 40;
    private static final double HEIGHT = 22;
    private static final double THUMB = 16;

    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected");
    private final Region thumb = new Region();

    public Switch(boolean initial) {
        getStyleClass().add("switch");
        Region track = new Region();
        track.getStyleClass().add("switch-track");
        track.setPrefSize(WIDTH, HEIGHT);
        track.setMaxSize(WIDTH, HEIGHT);
        thumb.getStyleClass().add("switch-thumb");
        thumb.setPrefSize(THUMB, THUMB);
        thumb.setMaxSize(THUMB, THUMB);
        thumb.setMouseTransparent(true);
        getChildren().addAll(track, thumb);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        selected.set(initial);
        thumb.setTranslateX(offset(initial));
        pseudoClassStateChanged(ON, initial);
        selected.addListener((obs, was, now) -> {
            pseudoClassStateChanged(ON, now);
            TranslateTransition move = new TranslateTransition(Duration.millis(120), thumb);
            move.setToX(offset(now));
            move.play();
        });
        setOnMouseClicked(e -> {
            if (!isDisabled()) {
                selected.set(!selected.get());
            }
        });
    }

    private static double offset(boolean on) {
        double travel = (WIDTH - THUMB) / 2 - 3;
        return on ? travel : -travel;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }
}
