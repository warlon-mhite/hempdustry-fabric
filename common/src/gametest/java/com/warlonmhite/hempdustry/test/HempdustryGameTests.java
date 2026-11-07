package com.warlonmhite.hempdustry.test;

import com.google.common.collect.ImmutableList;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import com.warlonmhite.hempdustry.test.framework.TestFunction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Registers the game tests: every {@code @GameTest} method of {@link #CLASSES}, on every loader.
 *
 * <p>A test is an annotated method, as on 1.20.1. Minecraft 1.16.5 has no game-test server and no
 * annotation, so the tests become {@link TestFunction}s of the harness in {@code test.framework}
 * here, named as the newer lines name them -- {@code hempdustry-gametest:<class>.<method>} -- and run
 * by it. Every test stands in a room of {@link #EMPTY}'s size, 8×8×8 of air.
 *
 * <p>A class with a test in it that is on no list is a hard startup failure, rather than a test that
 * silently never runs.
 *
 * <p><b>This list is branch-specific, and that is deliberate.</b> A feature branch adds its own
 * tests here and brings them along when it merges; nothing on this branch may reference a class
 * that only exists on another one, or the whole source set stops compiling.
 */
public final class HempdustryGameTests {
    public static final String NAMESPACE = "hempdustry-gametest";
    /** The structure every test stands in: 8×8×8 of air. */
    public static final String EMPTY = NAMESPACE + ":empty";

    /** Every test class that runs on every loader. */
    public static final List<Class<?>> CLASSES = ImmutableList.of(
            AddonEventGameTest.class,
            AnimalFoodGameTest.class,
            AdvancementGameTest.class,
            ApiBridgeGameTest.class,
            BalanceGameTest.class,
            BannerPatternGameTest.class,
            BeldiaGameTest.class,
            BongPlacementGameTest.class,
            BongRipGameTest.class,
            CharasGameTest.class,
            ConcentrateGameTest.class,
            ConfigGameTest.class,
            CreativeTabGameTest.class,
            CreeperSeeksHempGameTest.class,
            CropGrowthGameTest.class,
            DatapackStrainGameTest.class,
            DefoliationGameTest.class,
            FrozenSurfaceGameTest.class,
            FurnitureGameTest.class,
            GlassBongGameTest.class,
            HashishBarGameTest.class,
            HempOutfitGameTest.class,
            HempPressGameTest.class,
            IndoorGrowGameTest.class,
            InfuserBreakGameTest.class,
            InfuserPourGameTest.class,
            LangGameTest.class,
            LootGameTest.class,
            PackingGameTest.class,
            PlantStressGameTest.class,
            PlatformGameTest.class,
            RebalanceGameTest.class,
            SchwagGameTest.class,
            ScorchedHempGameTest.class,
            SiftingBoxGameTest.class,
            SweepGameTest.class,
            TagBackedBehaviourGameTest.class,
            TallPlantLootGameTest.class,
            VaporizerGameTest.class,
            ViewerGameTest.class);

    private HempdustryGameTests() {
    }

    /**
     * Every test of {@link #CLASSES} and of the loader's own {@code extra} classes, in the order they
     * run: class by class as listed, each class's tests by name.
     */
    public static List<TestFunction> all(Class<?>... extra) {
        List<Class<?>> classes = new ArrayList<>(CLASSES);
        classes.addAll(Arrays.asList(extra));
        checkEveryTestClassIsListed(classes);
        List<TestFunction> functions = new ArrayList<>();
        for (Class<?> testClass : classes) {
            Stream.of(testClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(GameTest.class))
                    .sorted(Comparator.comparing(Method::getName))
                    .forEach(method -> functions.add(testFunction(method)));
        }
        return functions;
    }

    private static TestFunction testFunction(Method method) {
        GameTest test = method.getAnnotation(GameTest.class);
        String name = NAMESPACE + ":" + method.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT)
                + "." + method.getName().toLowerCase(Locale.ROOT);
        return new TestFunction(test.batch(), name, test.timeoutTicks(), test.setupTicks(), test.required(),
                helper -> invoke(method, helper));
    }

    /** As vanilla's own test consumer: a fresh instance per run, and an assertion thrown as itself. */
    private static void invoke(Method method, GameTestHelper helper) {
        try {
            Object instance = Modifier.isStatic(method.getModifiers()) ? null
                    : method.getDeclaringClass().getDeclaredConstructor().newInstance();
            method.invoke(instance, helper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Every {@code *GameTest} class the test mod ships, against the classes registered. */
    private static void checkEveryTestClassIsListed(List<Class<?>> classes) {
        Set<String> listed = new HashSet<>();
        classes.forEach(c -> listed.add(c.getName()));
        List<String> missing = new ArrayList<>();
        // The test mod's roots, and the folder or jar these classes were loaded from: on Fabric the
        // first is only the test mod's resources.
        Set<Path> roots = new HashSet<>(TestPlatform.INSTANCE.testModRoots());
        try {
            roots.add(Paths.get(HempdustryGameTests.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException | RuntimeException e) {
            // A class loaded from somewhere that is no path: the test mod's roots are all there is.
        }
        for (Path root : roots) {
            Path dir = root.resolve(HempdustryGameTests.class.getPackage().getName().replace('.', '/'));
            if (!Files.isDirectory(dir)) {
                continue;
            }
            // this package only: the harness beside it (test.framework) holds no tests
            try (Stream<Path> walk = Files.walk(dir, 1)) {
                walk.map(path -> root.relativize(path).toString().replace('\\', '/'))
                        .filter(path -> path.endsWith("GameTest.class"))
                        .map(path -> path.substring(0, path.length() - ".class".length()).replace('/', '.'))
                        .filter(name -> !listed.contains(name) && !missing.contains(name))
                        .forEach(missing::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Game test classes registered by nobody: " + missing);
        }
    }
}
