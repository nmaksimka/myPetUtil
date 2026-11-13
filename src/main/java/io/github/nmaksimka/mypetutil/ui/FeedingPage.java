package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.HungerSpeed;
import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.pet.Pet;
import io.github.nmaksimka.mypetutil.sprite.FoodType;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Вкладка «Еда»: сытость и кормушка, из которой еду тащат на питомцев. */
final class FeedingPage implements Page {

    private final AppContext ctx;
    private final VBox root;

    FeedingPage(AppContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.settings();
        root = UiKit.page("Еда", "Перетащите еду из кормушки на питомца. Каждый ест только свою.");

        FlowPane tray = new FlowPane(4, 4);
        for (FoodType food : FoodType.values()) {
            tray.getChildren().add(foodTile(food));
        }
        VBox trayBox = new VBox(tray);
        trayBox.getStyleClass().add("group");
        trayBox.setStyle("-fx-padding: 6;");

        Switch hunger = new Switch(s.hungerEnabled);
        HBox speed = UiKit.segmented(List.of(HungerSpeed.values()), HungerSpeed::title, s.hungerSpeed, value -> {
            s.hungerSpeed = value;
            ctx.settingsChanged();
        });
        speed.setDisable(!s.hungerEnabled);
        hunger.selectedProperty().addListener((obs, old, now) -> {
            s.hungerEnabled = now;
            speed.setDisable(!now);
            ctx.settingsChanged();
        });

        root.getChildren().addAll(
                trayBox,
                left(UiKit.quiet("Покормить всех", this::feedAll)),
                UiKit.heading("Голод"),
                UiKit.group(
                        UiKit.row("Питомцы голодают",
                                "Когда сытость кончится, питомец сядет и загрустит, пока его не покормят.", hunger),
                        UiKit.row("Как быстро", "За 6 часов, 3 часа или полчаса.", speed)));
    }

    private static HBox left(Node node) {
        return new HBox(node);
    }

    private Node foodTile(FoodType food) {
        String eaters = Arrays.stream(PetCharacter.values())
                .filter(c -> c.food() == food)
                .map(PetCharacter::title)
                .collect(Collectors.joining(", "));
        VBox tile = new VBox(SpritePreview.still(food.grid(), 4.4),
                UiKit.text(food.title(), "name"),
                UiKit.label(eaters, "muted", "small"));
        tile.getStyleClass().add("food");

        tile.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                tile.setCursor(Cursor.CLOSED_HAND);
                ctx.pets().beginFoodDrag(food, e.getScreenX(), e.getScreenY());
            }
        });
        tile.setOnMouseDragged(e -> ctx.pets().continueFoodDrag(e.getScreenX(), e.getScreenY()));
        tile.setOnMouseReleased(e -> {
            tile.setCursor(Cursor.OPEN_HAND);
            ctx.pets().endFoodDrag(e.getScreenX(), e.getScreenY());
        });
        return tile;
    }

    private void feedAll() {
        for (Pet pet : ctx.pets().pets()) {
            ctx.pets().dropFoodNear(pet.entry().id);
        }
    }

    @Override
    public String title() {
        return "Еда";
    }

    @Override
    public Node view() {
        return root;
    }
}
