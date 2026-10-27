package dev.notsume.cinematictacz.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.notsume.cinematictacz.CinematicTacz;
import dev.notsume.cinematictacz.tacz.Tacz;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player's settings (config/cinematic_tacz-client.json), the way Cinematic Interactions keeps its own on Fabric: one
 * list of options, each in a section and with a translation key (its tooltip says what it does), read on start, saved
 * from {@link SettingsScreen}; the file is written with every value on first start.
 */
public final class Settings {
    private Settings() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(CinematicTacz.ID);
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(CinematicTacz.ID + "-client.json");

    /** An on/off setting: "section.name" in the file, cinematic_tacz.configuration.name (and .tooltip) on screen. */
    public record Option(String section, String name, boolean fallback) {
        String key() {
            return section + "." + name;
        }

        String translation() {
            return CinematicTacz.ID + ".configuration." + name;
        }
    }

    static final Option GUN_SMITH_TABLE = new Option("scenes", "gunSmithTable", true);
    static final Option AMMO_BENCH = new Option("scenes", "ammoBench", true);
    static final Option ATTACHMENT_BENCH = new Option("scenes", "attachmentBench", true);
    static final Option ANIMATIONS = new Option("crafting", "animations", true);
    static final Option INSPECT = new Option("crafting", "inspect", true);
    static final Option PARTICLES = new Option("effects", "particles", true);
    /** In the order (and sections) of the file and the screen. */
    static final List<Option> ALL = List.of(GUN_SMITH_TABLE, AMMO_BENCH, ATTACHMENT_BENCH, ANIMATIONS, INSPECT, PARTICLES);

    private static final Map<Option, Boolean> VALUES = new HashMap<>();

    public static boolean get(Option option) {
        return VALUES.getOrDefault(option, option.fallback());
    }

    static void set(Option option, boolean value) {
        VALUES.put(option, value);
    }

    /** Whether a table of this shape plays its scene; off, it keeps TACZ's own window. */
    public static boolean scene(Tacz.Shape shape) {
        return get(switch (shape) {
            case SINGLE -> AMMO_BENCH;
            case WIDE -> GUN_SMITH_TABLE;
            case TALL -> ATTACHMENT_BENCH;
        });
    }

    static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try (var reader = Files.newBufferedReader(PATH)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Option option : ALL) {
                if (root.has(option.key())) VALUES.put(option, root.get(option.key()).getAsBoolean());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read {}; using the defaults", PATH, e);
        }
    }

    static void save() {
        JsonObject root = new JsonObject();
        for (Option option : ALL) root.addProperty(option.key(), get(option));
        try {
            Files.createDirectories(PATH.getParent());
            try (var writer = Files.newBufferedWriter(PATH)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not save {}", PATH, e);
        }
    }
}
