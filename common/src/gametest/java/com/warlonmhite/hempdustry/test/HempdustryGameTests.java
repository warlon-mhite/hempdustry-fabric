package com.warlonmhite.hempdustry.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Registers the game tests: every {@code @GameTest} method of {@link #CLASSES}, on every loader.
 *
 * <p>On this version a test is an annotated method, and each loader reads the annotation its own way
 * -- Fabric takes the template as a structure id, NeoForge prefixes it with a namespace and the
 * class's name unless told otherwise in annotations of its own. So neither reads ours: each loader's
 * test mod hands {@link #register} the classes, and the tests become vanilla's {@code TestFunction}s
 * here, named and placed the way Fabric names them -- {@code hempdustry-gametest:<class>.<method>},
 * in the structure the template names. The tests stand in {@link #EMPTY}, an 8×8×8 structure of air
 * shipped beside them, rather than a structure of one loader's.
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
    public static final List<Class<?>> CLASSES = List.of(
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
     * Adds every test of {@link #CLASSES} and of the loader's own {@code extra} classes to vanilla's
     * registry, which the game test server runs. Call once, before the server starts.
     */
    public static void register(Class<?>... extra) {
        List<Class<?>> classes = new ArrayList<>(CLASSES);
        classes.addAll(List.of(extra));
        checkEveryTestClassIsListed(classes);
        for (Class<?> testClass : classes) {
            Stream.of(testClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(GameTest.class))
                    .sorted(Comparator.comparing(Method::getName))
                    .forEach(method -> GameTestRegistry.getAllTestFunctions().add(testFunction(method)));
        }
    }

    private static TestFunction testFunction(Method method) {
        GameTest test = method.getAnnotation(GameTest.class);
        String name = NAMESPACE + ":" + method.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT)
                + "." + method.getName().toLowerCase(Locale.ROOT);
        return new TestFunction(test.batch(), name, test.template().isEmpty() ? name : test.template(),
                StructureUtils.getRotationForRotationSteps(test.rotationSteps()), test.timeoutTicks(),
                test.setupTicks(), test.required(), test.manualOnly(), test.attempts(), test.requiredSuccesses(),
                test.skyAccess(), helper -> invoke(method, helper));
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
            roots.add(Path.of(HempdustryGameTests.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException | RuntimeException e) {
            // A class loaded from somewhere that is no path: the test mod's roots are all there is.
        }
        for (Path root : roots) {
            Path dir = root.resolve(HempdustryGameTests.class.getPackageName().replace('.', '/'));
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
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
