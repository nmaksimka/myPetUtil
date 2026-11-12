package io.github.nmaksimka.mypetutil.ui;

import io.github.nmaksimka.mypetutil.config.PetEntry;
import io.github.nmaksimka.mypetutil.pet.Pet;
import io.github.nmaksimka.mypetutil.pet.PetState;
import io.github.nmaksimka.mypetutil.sprite.Anim;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.sprite.Pose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Вкладка «Питомцы»: галерея персонажей и список тех, кто уже на экране. */
final class PetsPage implements Page {

    private record RowRefs(Pet pet, Region fill, Label status) {
    }

    private final AppContext ctx;
    private final VBox root;
    private final VBox list = new VBox();
    private final List<SpritePreview> previews = new ArrayList<>();
    private final Map<String, RowRefs> rows = new HashMap<>();
    private double lastRowsUpdate;
    private int shownCount = -1;
    private boolean shownHunger;

    PetsPage(AppContext ctx) {
        this.ctx = ctx;
        root = UiKit.page("Питомцы",
                "Выберите, кого выпустить на экран. Можно сколько угодно — хоть целую толпу. "
                        + "Питомца можно взять мышкой, перенести и даже подбросить.");

        FlowPane gallery = new FlowPane(16, 16);
        for (PetCharacter character : PetCharacter.values()) {
            gallery.getChildren().add(card(character));
        }

        Button removeAll = UiKit.danger("Убрать всех", () -> {
            ctx.pets().removeAll();
            rebuildList();
        });
        HBox listHeader = new HBox(UiKit.section("Сейчас на экране"), UiKit.spacer(), removeAll);
        listHeader.setAlignment(Pos.BOTTOM_LEFT);

        list.getStyleClass().add("list");
        root.getChildren().addAll(UiKit.section("Кого выпустить"), gallery, listHeader, list);
    }

    private Node card(PetCharacter character) {
        SpritePreview preview = new SpritePreview(character, 6);
        previews.add(preview);
        StackPane previewBox = new StackPane(preview);
        previewBox.getStyleClass().add("preview-box");
        previewBox.setPrefHeight(118);
        previewBox.setAlignment(Pos.BOTTOM_CENTER);
        previewBox.setStyle("-fx-padding: 0 0 6 0;");

        Label food = UiKit.label("Любит: " + character.food().title(), "chip");
        food.setGraphic(SpritePreview.still(character.food().grid(), 1.6));
        food.setGraphicTextGap(6);

        Button release = UiKit.primary("Выпустить", () -> {
            ctx.pets().addPet(character);
            rebuildList();
        });
        release.setMaxWidth(Double.MAX_VALUE);

        Region grow = new Region();
        VBox.setVgrow(grow, javafx.scene.layout.Priority.ALWAYS);
        VBox card = new VBox(previewBox,
                UiKit.label(character.title(), "strong"),
                UiKit.text(character.description(), "muted", "small"),
                food, grow, release);
        card.getStyleClass().addAll("card", "pet-card");

        // при наведении на карточку питомец начинает идти
        card.setOnMouseEntered(e -> preview.setAnim(Anim.WALK, now()));
        card.setOnMouseExited(e -> preview.setAnim(Anim.IDLE, now()));
        return card;
    }

    private static double now() {
        return System.nanoTime() / 1e9;
    }

    private void rebuildList() {
        list.getChildren().clear();
        rows.clear();
        List<Pet> pets = ctx.pets().pets();
        shownCount = pets.size();
        shownHunger = ctx.settings().hungerEnabled;
        if (pets.isEmpty()) {
            list.getChildren().add(UiKit.text("Пока никого нет. Выпустите кого-нибудь из галереи выше.", "empty"));
            return;
        }
        for (int i = 0; i < pets.size(); i++) {
            HBox row = row(pets.get(i));
            if (i == pets.size() - 1) {
                row.getStyleClass().add("last");
            }
            list.getChildren().add(row);
        }
        updateRows();
    }

    private HBox row(Pet pet) {
        PetEntry entry = pet.entry();
        PetCharacter character = pet.character();
        Node icon = SpritePreview.still(character.art().frame(Pose.of(Anim.IDLE, 0)), 2.25);

        Label status = UiKit.label("", "muted", "small");
        VBox info = new VBox(4, UiKit.label(character.title(), "strong"));
        Region fill = null;
        if (ctx.settings().hungerEnabled) {
            Region track = new Region();
            track.getStyleClass().add("satiety-track");
            track.setPrefSize(140, 6);
            track.setMaxSize(140, 6);
            fill = new Region();
            fill.getStyleClass().add("satiety-fill");
            fill.setPrefHeight(6);
            fill.setMaxHeight(6);
            StackPane bar = new StackPane(track, fill);
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setMaxWidth(140);
            HBox satiety = new HBox(8, bar, status);
            satiety.setAlignment(Pos.CENTER_LEFT);
            info.getChildren().add(satiety);
        } else {
            info.getChildren().add(status);
        }

        HBox row = new HBox(icon, info, UiKit.spacer(),
                UiKit.flat("Покормить", () -> ctx.pets().dropFoodNear(entry.id)),
                UiKit.flat("Позвать", () -> ctx.pets().callPet(entry.id)),
                UiKit.danger("Убрать", () -> {
                    ctx.pets().removePet(entry.id);
                    rebuildList();
                }));
        row.getStyleClass().add("row");
        rows.put(entry.id, new RowRefs(pet, fill, status));
        return row;
    }

    private void updateRows() {
        for (RowRefs refs : rows.values()) {
            Pet pet = refs.pet();
            if (refs.fill() != null) {
                double part = pet.satiety() / PetEntry.MAX_SATIETY;
                refs.fill().setPrefWidth(Math.max(2, 140 * part));
                refs.fill().setMaxWidth(Math.max(2, 140 * part));
                refs.fill().getStyleClass().removeAll("mid", "low");
                if (part <= 0.2) {
                    refs.fill().getStyleClass().add("low");
                } else if (part <= 0.5) {
                    refs.fill().getStyleClass().add("mid");
                }
                refs.status().setText(Math.round(part * 100) + "% · " + describe(pet.state()));
            } else {
                refs.status().setText(describe(pet.state()));
            }
        }
    }

    static String describe(PetState state) {
        return switch (state) {
            case IDLE -> "отдыхает";
            case WALK, EXIT, ENTER -> "гуляет";
            case HOVER, HAPPY -> "радуется";
            case DRAG -> "у вас в руках";
            case FALL -> "летит";
            case LAND -> "приземлился";
            case EAT -> "кушает";
            case SAD -> "голодный, грустит";
        };
    }

    @Override
    public String title() {
        return "Питомцы";
    }

    @Override
    public Node view() {
        return root;
    }

    @Override
    public void onShow() {
        rebuildList();
    }

    @Override
    public void tick(double now) {
        for (SpritePreview preview : previews) {
            preview.tick(now);
        }
        if (now - lastRowsUpdate > 0.5) {
            lastRowsUpdate = now;
            if (ctx.pets().pets().size() != shownCount || ctx.settings().hungerEnabled != shownHunger) {
                rebuildList();
            } else {
                updateRows();
            }
        }
    }
}
