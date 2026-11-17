package io.github.nmaksimka.mypetutil.overlay;

import io.github.nmaksimka.mypetutil.config.BehaviorMode;
import io.github.nmaksimka.mypetutil.config.PetEntry;
import io.github.nmaksimka.mypetutil.config.Settings;
import io.github.nmaksimka.mypetutil.pet.FoodItem;
import io.github.nmaksimka.mypetutil.pet.Pet;
import io.github.nmaksimka.mypetutil.pet.PetRules;
import io.github.nmaksimka.mypetutil.platform.DesktopSnapshot;
import io.github.nmaksimka.mypetutil.platform.ScreenMapper;
import io.github.nmaksimka.mypetutil.platform.ScreenMapper.FxScreen;
import io.github.nmaksimka.mypetutil.platform.WindowScanner;
import io.github.nmaksimka.mypetutil.sprite.FoodType;
import io.github.nmaksimka.mypetutil.sprite.PetCharacter;
import io.github.nmaksimka.mypetutil.world.Rect;
import io.github.nmaksimka.mypetutil.world.ScreenInfo;
import io.github.nmaksimka.mypetutil.world.World;
import io.github.nmaksimka.mypetutil.world.WorldBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Управляет всеми питомцами и едой: игровой цикл 60 раз в секунду,
 * перетаскивание мышкой, добавление и удаление.
 */
public final class PetManager {

    private static final boolean DEBUG = Boolean.getBoolean("mypetutil.debug");

    private record Sample(long nanos, double x, double y) {
    }

    private final Settings settings;
    private final Runnable onSettingsChanged;
    private final WindowScanner scanner;
    private final Stage owner;
    private final Random rnd = new Random();

    private final List<PetView> pets = new ArrayList<>();
    private final List<FoodView> foods = new ArrayList<>();
    private final Deque<Sample> dragSamples = new ArrayDeque<>();

    private World world;
    private DesktopSnapshot lastSnapshot;
    private int lastScale;
    private boolean lastWindowsMode;
    private boolean userHidden;
    private boolean autoHidden;
    private long lastNanos;
    private double time;
    private double topmostTimer;
    private double saveTimer;
    private double debugTimer;

    private PetView dragged;
    private double grabDx;
    private double grabDy;
    private FoodView draggedFood;

    private final AnimationTimer loop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            tick(now);
        }
    };

    public PetManager(Settings settings, WindowScanner scanner, Runnable onSettingsChanged) {
        this.settings = settings;
        this.scanner = scanner;
        this.onSettingsChanged = onSettingsChanged;

        // скрытое окно-хозяин: окна с хозяином не появляются на панели задач
        owner = new Stage(StageStyle.UTILITY);
        owner.setTitle(WindowScanner.OWN_WINDOW_PREFIX + "owner");
        owner.setOpacity(0);
        owner.setWidth(1);
        owner.setHeight(1);
        owner.setX(-32000);
        owner.setY(-32000);
        owner.show();
    }

    public void start() {
        world = buildWorld();
        for (PetEntry entry : settings.pets) {
            spawn(entry);
        }
        loop.start();
    }

    public void stop() {
        loop.stop();
        pets.forEach(PetView::close);
        foods.forEach(FoodView::close);
        owner.close();
    }

    // ---------- питомцы ----------

    public void addPet(PetCharacter character) {
        PetEntry entry = new PetEntry(character);
        settings.pets.add(entry);
        spawn(entry);
        onSettingsChanged.run();
    }

    private void spawn(PetEntry entry) {
        World w = world != null ? world : buildWorld();
        // появляется сверху случайного монитора и падает вниз
        ScreenInfo screen = w.screens().get(rnd.nextInt(w.screens().size()));
        double size = 16 * settings.scale;
        Rect v = screen.visual();
        double x = v.minX() + 40 + rnd.nextDouble() * Math.max(1, v.width() - size - 80);
        double y = v.minY() + size + 20;
        Pet pet = new Pet(entry, x, y);
        PetView view = new PetView(owner, pet, settings.scale, dragListener());
        pets.add(view);
        view.showAtPet();
        view.setVisible(!userHidden && !autoHidden);
    }

    public void removePet(String id) {
        pets.removeIf(view -> {
            if (view.pet().entry().id.equals(id)) {
                view.close();
                return true;
            }
            return false;
        });
        settings.pets.removeIf(p -> p.id.equals(id));
        onSettingsChanged.run();
    }

    public void removeAll() {
        pets.forEach(PetView::close);
        pets.clear();
        settings.pets.clear();
        onSettingsChanged.run();
    }

    /** Позвать питомца: он падает сверху в центр основного монитора. */
    public void callPet(String id) {
        for (PetView view : pets) {
            if (view.pet().entry().id.equals(id)) {
                Rectangle2D v = Screen.getPrimary().getVisualBounds();
                double size = 16 * settings.scale;
                view.pet().startDrag();
                view.pet().dragTo(v.getMinX() + v.getWidth() / 2 - size / 2, v.getMinY() + size + 40, 0);
                view.pet().release(0, 0);
            }
        }
    }

    public List<Pet> pets() {
        return pets.stream().map(PetView::pet).toList();
    }

    public boolean isUserHidden() {
        return userHidden;
    }

    public void setUserHidden(boolean hidden) {
        userHidden = hidden;
        applyVisibility();
    }

    private void applyVisibility() {
        boolean visible = !userHidden && !autoHidden;
        pets.forEach(v -> v.setVisible(visible));
        foods.forEach(v -> v.setVisible(visible));
    }

    // ---------- еда ----------

    /** Еда появилась под курсором и сразу «в руке» (её тащат из окна настроек). */
    public void beginFoodDrag(FoodType type, double screenX, double screenY) {
        double size = FoodItem.size(settings.scale);
        FoodItem food = new FoodItem(type, screenX - size / 2, screenY + size / 2);
        FoodView view = new FoodView(owner, food, settings.scale, foodListener());
        foods.add(view);
        view.showAtFood();
        draggedFood = view;
        dragSamples.clear();
        sample(screenX, screenY);
    }

    public void continueFoodDrag(double screenX, double screenY) {
        if (draggedFood != null) {
            double size = FoodItem.size(settings.scale);
            draggedFood.food().moveTo(screenX - size / 2, screenY + size / 2);
            sample(screenX, screenY);
        }
    }

    public void endFoodDrag(double screenX, double screenY) {
        if (draggedFood == null) {
            return;
        }
        FoodItem food = draggedFood.food();
        draggedFood = null;
        double size = 16 * settings.scale;
        Rect foodRect = food.bounds(settings.scale);
        for (PetView view : pets) {
            Pet pet = view.pet();
            if (pet.bounds(size).intersects(foodRect) && pet.offer(food)) {
                return;
            }
        }
        double[] v = velocity();
        food.drop(v[0] * 0.6, Math.min(v[1] * 0.6, 600));
    }

    /** Кнопка «Покормить»: еда падает сверху прямо перед питомцем. */
    public void dropFoodNear(String petId) {
        for (PetView view : pets) {
            Pet pet = view.pet();
            if (pet.entry().id.equals(petId)) {
                double size = 16 * settings.scale;
                double foodSize = FoodItem.size(settings.scale);
                double x = pet.x() + size / 2 - foodSize / 2 + pet.facing() * size;
                FoodItem food = new FoodItem(pet.character().food(), x, pet.y() - size * 3);
                food.drop(0, 0);
                FoodView foodView = new FoodView(owner, food, settings.scale, foodListener());
                foods.add(foodView);
                foodView.showAtFood();
            }
        }
    }

    private FoodView.Listener foodListener() {
        return new FoodView.Listener() {
            @Override
            public void pressed(FoodView view, double screenX, double screenY) {
                // схватили лежащую или падающую еду
                draggedFood = view;
                view.food().hold();
                dragSamples.clear();
                sample(screenX, screenY);
            }

            @Override
            public void dragged(FoodView view, double screenX, double screenY) {
                continueFoodDrag(screenX, screenY);
            }

            @Override
            public void released(FoodView view, double screenX, double screenY) {
                endFoodDrag(screenX, screenY);
            }
        };
    }

    // ---------- перетаскивание питомца ----------

    private PetView.DragListener dragListener() {
        return new PetView.DragListener() {
            @Override
            public void pressed(PetView view, double screenX, double screenY) {
                dragged = view;
                Pet pet = view.pet();
                grabDx = screenX - pet.x();
                grabDy = screenY - pet.y();
                pet.startDrag();
                dragSamples.clear();
                sample(screenX, screenY);
            }

            @Override
            public void dragged(PetView view, double screenX, double screenY) {
                if (dragged != view) {
                    return;
                }
                sample(screenX, screenY);
                view.pet().dragTo(screenX - grabDx, screenY - grabDy, velocity()[0]);
            }

            @Override
            public void released(PetView view, double screenX, double screenY) {
                if (dragged != view) {
                    return;
                }
                sample(screenX, screenY);
                double[] v = velocity();
                view.pet().release(v[0], v[1]);
                dragged = null;
            }
        };
    }

    private void sample(double x, double y) {
        long now = System.nanoTime();
        dragSamples.addLast(new Sample(now, x, y));
        while (dragSamples.size() > 2 && now - dragSamples.peekFirst().nanos() > 100_000_000L) {
            dragSamples.removeFirst();
        }
    }

    /** Скорость мышки за последние ~100 мс — с ней питомца и бросаем. */
    private double[] velocity() {
        if (dragSamples.size() < 2) {
            return new double[] {0, 0};
        }
        Sample first = dragSamples.peekFirst();
        Sample last = dragSamples.peekLast();
        double seconds = (last.nanos() - first.nanos()) / 1e9;
        if (seconds < 0.01) {
            return new double[] {0, 0};
        }
        return new double[] {(last.x() - first.x()) / seconds, (last.y() - first.y()) / seconds};
    }

    // ---------- игровой цикл ----------

    private void tick(long now) {
        double dt = lastNanos == 0 ? 1.0 / 60 : Math.min((now - lastNanos) / 1e9, 0.05);
        lastNanos = now;
        time += dt;

        DesktopSnapshot snapshot = scanner.latest();
        boolean windowsMode = settings.mode == BehaviorMode.WINDOWS;
        if (snapshot != lastSnapshot || settings.scale != lastScale || windowsMode != lastWindowsMode) {
            lastSnapshot = snapshot;
            lastScale = settings.scale;
            lastWindowsMode = windowsMode;
            world = buildWorld();
            updateAutoHide(snapshot);
        }

        PetRules rules = PetRules.from(settings);
        List<FoodItem> foodItems = foods.stream().map(FoodView::food).toList();
        for (PetView view : pets) {
            view.setPixel(settings.scale);
            view.pet().update(dt, world, rules, foodItems, rnd);
            if (!userHidden && !autoHidden) {
                view.render(settings.hungerEnabled, time);
            }
        }
        foods.removeIf(view -> {
            FoodItem food = view.food();
            view.setPixel(settings.scale);
            food.update(dt, world, settings.scale, rules.walksOnWindows());
            if (food.gone()) {
                view.close();
                if (draggedFood == view) {
                    draggedFood = null;
                }
                return true;
            }
            view.render();
            return false;
        });

        if (DEBUG) {
            debugTimer -= dt;
            if (debugTimer <= 0) {
                debugTimer = 2;
                System.out.println("floors " + world.floors());
                pets.forEach(v -> System.out.printf("%s %s x=%.0f y=%.0f owner=%s stage=%.0f,%.0f actual=%s%n",
                        v.pet().character(), v.pet().state(), v.pet().x(), v.pet().y(), v.pet().owner(),
                        v.stageX(), v.stageY(), v.actualPosition()));
            }
        }

        topmostTimer -= dt;
        if (topmostTimer <= 0) {
            topmostTimer = snapshot.fullscreenRect() != null ? 1.0 : 5.0;
            pets.forEach(OverlayStage::bringToTop);
        }
        if (settings.hungerEnabled) {
            saveTimer -= dt;
            if (saveTimer <= 0) {
                saveTimer = 20;
                onSettingsChanged.run();
            }
        }
    }

    /** Если запрещённая программа открыта на весь экран (например, игра) — прячемся. */
    private void updateAutoHide(DesktopSnapshot snapshot) {
        boolean hide = settings.hideOnBlockedFullscreen
                && snapshot.fullscreenRect() != null
                && snapshot.foregroundExe() != null
                && settings.blockedApps.contains(snapshot.foregroundExe());
        if (hide != autoHidden) {
            autoHidden = hide;
            applyVisibility();
        }
    }

    private World buildWorld() {
        List<FxScreen> fxScreens = new ArrayList<>();
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D b = screen.getBounds();
            Rectangle2D v = screen.getVisualBounds();
            fxScreens.add(new FxScreen(
                    new Rect(b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY()),
                    new Rect(v.getMinX(), v.getMinY(), v.getMaxX(), v.getMaxY()),
                    screen.equals(Screen.getPrimary())));
        }
        ScreenMapper.Mapped mapped = ScreenMapper.map(scanner.latest(), fxScreens);
        OverlayStage.setMapping(mapped);
        double size = 16 * settings.scale;
        return WorldBuilder.build(mapped.screens(), mapped.windows(),
                settings.mode == BehaviorMode.WINDOWS,
                settings.blockedApps, size, size);
    }

    /** Список программ с окнами прямо сейчас — для вкладки «Программы». */
    public List<DesktopSnapshot.RawWindow> currentWindows() {
        return scanner.latest().windows();
    }
}
