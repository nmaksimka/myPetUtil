package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.HungerSpeed;
import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.pet.Pet;
import io.github.nmaksimka.mypetutil.sprite.FoodType;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;

import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Вкладка «Кормление»: сытость и кормушка, из которой еду тащат на питомцев. */
final class FeedingPage implements Page {

    private final AppContext ctx;
    private final VBox root;

    FeedingPage(AppContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.settings();
        root = UiKit.page("Кормление",
                "У каждого питомца своя любимая еда. Чужую он не тронет — только удивлённо посмотрит.");

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

        VBox speedBox = new VBox(10, UiKit.label("Как быстро голодают", "strong"),
                UiKit.text("Медленно — за 6 часов, обычно — за 3 часа, быстро — за полчаса.", "muted", "small"), speed);
        speedBox.getStyleClass().add("card");

        FlowPane tray = new FlowPane(12, 12);
        for (FoodType food : FoodType.values()) {
            tray.getChildren().add(foodTile(food));
        }

        root.getChildren().addAll(
                UiKit.section("Сытость"),
                UiKit.settingRow("Питомцы могут проголодаться",
                        "Если включить, сытость понемногу убывает. Когда она кончится, питомец сядет и загрустит, "
                                + "пока его не покормят. Если выключить — питомцы всегда сыты, а еда просто в радость.",
                        hunger),
                speedBox,
                UiKit.section("Кормушка"),
                UiKit.text("Зажмите еду мышкой и перетащите прямо на питомца. Если отпустить мимо — еда упадёт, "
                        + "и голодный питомец сам к ней подойдёт.", "muted"),
                tray,
                UiKit.button("Покормить всех", this::feedAll));
    }

    private Node foodTile(FoodType food) {
        String eaters = java.util.Arrays.stream(PetCharacter.values())
                .filter(c -> c.food() == food)
                .map(PetCharacter::title)
                .collect(Collectors.joining(", "));
        VBox tile = new VBox(SpritePreview.still(food.grid(), 4.8),
                UiKit.label(food.title(), "strong"),
                UiKit.label(eaters, "muted", "small"));
        tile.getStyleClass().add("food-tile");

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
        return "Кормление";
    }

    @Override
    public Node view() {
        return root;
    }
}
