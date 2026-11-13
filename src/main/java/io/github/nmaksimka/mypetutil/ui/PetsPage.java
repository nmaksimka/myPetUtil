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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Вкладка «Питомцы». Сверху полка, на которой стоят все персонажи — кликом выбирается,
 * кого выпустить. Ниже список тех, кто уже гуляет по экрану.
 */
final class PetsPage implements Page {

    private record Slot(PetCharacter character, VBox node, SpritePreview preview) {
    }

    private record RowRefs(Pet pet, Region fill, Label status) {
    }

    private final AppContext ctx;
    private final VBox root;
    private final List<Slot> slots = new ArrayList<>();
    private final Label selectedName = UiKit.label("", "title");
    private final Label selectedText = UiKit.text("", "muted");
    private final Label selectedFood = UiKit.label("", "muted");
    private final Button release;
    private final VBox list = new VBox();
    private final Map<String, RowRefs> rows = new HashMap<>();
    private PetCharacter selected = PetCharacter.CLAUDE;
    private double lastRowsUpdate;
    private int shownCount = -1;
    private boolean shownHunger;

    PetsPage(AppContext ctx) {
        this.ctx = ctx;
        root = UiKit.page("Питомцы", null);

        HBox shelfRow = new HBox(4);
        shelfRow.setAlignment(Pos.BOTTOM_CENTER);
        for (PetCharacter character : PetCharacter.values()) {
            shelfRow.getChildren().add(slot(character));
        }
        Region ground = new Region();
        ground.getStyleClass().add("shelf-ground");
        VBox shelf = new VBox(shelfRow, ground);
        shelf.getStyleClass().add("shelf");

        release = UiKit.button("", () -> {
            ctx.pets().addPet(selected);
            rebuildList();
        });
        VBox about = new VBox(4, selectedName, selectedText, selectedFood);
        HBox.setHgrow(about, Priority.ALWAYS);
        HBox details = new HBox(24, about, release);
        details.setAlignment(Pos.CENTER_LEFT);

        HBox listHeader = new HBox(UiKit.heading("На экране"), UiKit.spacer(),
                UiKit.quietDanger("Убрать всех", () -> {
                    ctx.pets().removeAll();
                    rebuildList();
                }));
        listHeader.setAlignment(Pos.BOTTOM_LEFT);
        list.getStyleClass().add("group");

        root.getChildren().addAll(shelf, details, listHeader, list,
                UiKit.text("Питомца можно взять мышкой, перенести и подбросить.", "muted", "small"));
        select(PetCharacter.CLAUDE);
    }

    private Node slot(PetCharacter character) {
        SpritePreview preview = new SpritePreview(character, 5);
        VBox node = new VBox(preview, UiKit.label(character.title(), "shelf-caption"));
        node.getStyleClass().add("shelf-slot");
        HBox.setHgrow(node, Priority.ALWAYS);
        node.setMaxWidth(Double.MAX_VALUE);
        node.setOnMouseClicked(e -> select(character));
        node.setOnMouseEntered(e -> preview.setAnim(Anim.WALK, now()));
        node.setOnMouseExited(e -> preview.setAnim(selected == character ? Anim.HOVER : Anim.IDLE, now()));
        slots.add(new Slot(character, node, preview));
        return node;
    }

    private void select(PetCharacter character) {
        selected = character;
        for (Slot slot : slots) {
            boolean on = slot.character() == character;
            slot.node().pseudoClassStateChanged(UiKit.SELECTED, on);
            slot.preview().setAnim(on ? Anim.HOVER : Anim.IDLE, now());
        }
        selectedName.setText(character.title());
        selectedText.setText(character.description());
        selectedFood.setText("Любимая еда: " + character.food().title().toLowerCase());
        selectedFood.setGraphic(SpritePreview.still(character.food().grid(), 2));
        release.setText("Выпустить");
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
            list.getChildren().add(UiKit.text("Здесь пусто. Выберите персонажа на полке и нажмите «Выпустить».", "empty"));
            return;
        }
        for (Pet pet : pets) {
            UiKit.addRow(list, row(pet));
        }
        updateRows();
    }

    private HBox row(Pet pet) {
        PetEntry entry = pet.entry();
        PetCharacter character = pet.character();
        Node icon = SpritePreview.still(character.art().frame(Pose.of(Anim.IDLE, 0)), 2);

        Label status = UiKit.label("", "muted", "small");
        VBox info = new VBox(3, UiKit.label(character.title(), "name"));
        Region fill = null;
        if (ctx.settings().hungerEnabled) {
            Region track = new Region();
            track.getStyleClass().add("satiety-track");
            track.setPrefSize(120, 4);
            track.setMaxSize(120, 4);
            fill = new Region();
            fill.getStyleClass().add("satiety-fill");
            fill.setPrefHeight(4);
            fill.setMaxHeight(4);
            StackPane bar = new StackPane(track, fill);
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setMaxWidth(120);
            HBox satiety = new HBox(10, bar, status);
            satiety.setAlignment(Pos.CENTER_LEFT);
            info.getChildren().add(satiety);
        } else {
            info.getChildren().add(status);
        }

        HBox row = new HBox(icon, info, UiKit.spacer(),
                UiKit.quiet("Покормить", () -> ctx.pets().dropFoodNear(entry.id)),
                UiKit.quiet("Позвать", () -> ctx.pets().callPet(entry.id)),
                UiKit.quietDanger("Убрать", () -> {
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
                refs.fill().setPrefWidth(Math.max(2, 120 * part));
                refs.fill().setMaxWidth(Math.max(2, 120 * part));
                refs.status().setText("сыт на " + Math.round(part * 100) + "%, " + describe(pet.state()));
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
            case EAT -> "ест";
            case SAD -> "проголодался";
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
        for (Slot slot : slots) {
            slot.preview().tick(now);
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
