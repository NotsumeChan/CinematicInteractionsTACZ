package dev.notsume.cinematictacz.client;

import dev.notsume.cinematictacz.CinematicTacz;
import dev.notsume.cinematictacz.tacz.Tacz;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//? if fabric {
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?} else if neoforge {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?} else if forge {
/*import net.minecraftforge.common.ForgeConfigSpec;
*///?}

/**
 * The player's settings, the way Cinematic Interactions keeps its own: one list of options, each in a section, with a
 * comment and a translation key (its tooltip says what it does on screen), edited on {@link SettingsScreen}. On Fabric
 * they are config/cinematic_tacz-client.json, read on start and written with every value on first start; on (Neo)Forge a
 * config spec, config/cinematic_tacz-client.toml, which NeoForge reads and keeps.
 */
public final class Settings {
    private Settings() {}

    /** An on/off setting: "section.name" in the file, cinematic_tacz.configuration.name (and .tooltip) on screen. */
    public record Option(String section, String name, boolean fallback, String comment) {
        String key() {
            return section + "." + name;
        }

        String translation() {
            return CinematicTacz.ID + ".configuration." + name;
        }
    }

    static final Option GUN_SMITH_TABLE = new Option("scenes", "gunSmithTable", true,
            "Whether the gun smith table plays its scene (false: TACZ's own window).");
    static final Option AMMO_BENCH = new Option("scenes", "ammoBench", true,
            "Whether the ammo workbench plays its scene (false: TACZ's own window).");
    static final Option ATTACHMENT_BENCH = new Option("scenes", "attachmentBench", true,
            "Whether the attachment workbench plays its scene (false: TACZ's own window).");
    static final Option ANIMATIONS = new Option("crafting", "animations", true,
            "The crafting animations: hammer and drill, press, resin printer (false: crafted at once).");
    static final Option INSPECT = new Option("crafting", "inspect", true,
            "After forging a gun the scene closes and the gun is drawn and inspected (false: the scene stays open).");
    static final Option PARTICLES = new Option("effects", "particles", true,
            "The smoke, sparks and fire of the crafting animations.");
    /** In the order (and sections) of the file and the screen. */
    static final List<Option> ALL = List.of(GUN_SMITH_TABLE, AMMO_BENCH, ATTACHMENT_BENCH, ANIMATIONS, INSPECT, PARTICLES);

    /** Whether a table of this shape plays its scene; off, it keeps TACZ's own window. */
    public static boolean scene(Tacz.Shape shape) {
        return get(switch (shape) {
            case SINGLE -> AMMO_BENCH;
            case WIDE -> GUN_SMITH_TABLE;
            case TALL -> ATTACHMENT_BENCH;
        });
    }

    //? if fabric {
    private static final Logger LOGGER = LoggerFactory.getLogger(CinematicTacz.ID);
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(CinematicTacz.ID + "-client.json");
    private static final Map<Option, Boolean> VALUES = new HashMap<>();

    public static boolean get(Option option) {
        return VALUES.getOrDefault(option, option.fallback());
    }

    static void set(Option option, boolean value) {
        VALUES.put(option, value);
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
    //?} else if neoforge {
    /*public static final ModConfigSpec SPEC;
    private static final Map<Option, ModConfigSpec.BooleanValue> VALUES = new HashMap<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        String section = null;
        for (Option option : ALL) {
            if (!option.section().equals(section)) {
                if (section != null) builder.pop();
                section = option.section();
                builder.translation(CinematicTacz.ID + ".configuration." + section).push(section);
            }
            VALUES.put(option, builder.comment(option.comment()).translation(option.translation()).define(option.name(), option.fallback()));
        }
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean get(Option option) {
        // before the file is read (very early in loading) the defaults apply
        return SPEC.isLoaded() ? VALUES.get(option).get() : option.fallback();
    }

    static void set(Option option, boolean value) {
        VALUES.get(option).set(value);
    }

    static void save() {
        SPEC.save();
    }
    *///?} else if forge {
    /*public static final ForgeConfigSpec SPEC;
    private static final Map<Option, ForgeConfigSpec.BooleanValue> VALUES = new HashMap<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        String section = null;
        for (Option option : ALL) {
            if (!option.section().equals(section)) {
                if (section != null) builder.pop();
                section = option.section();
                builder.translation(CinematicTacz.ID + ".configuration." + section).push(section);
            }
            VALUES.put(option, builder.comment(option.comment()).translation(option.translation()).define(option.name(), option.fallback()));
        }
        builder.pop();
        SPEC = builder.build();
    }

    public static boolean get(Option option) {
        // before the file is read (very early in loading) the defaults apply
        return SPEC.isLoaded() ? VALUES.get(option).get() : option.fallback();
    }

    static void set(Option option, boolean value) {
        VALUES.get(option).set(value);
    }

    static void save() {
        SPEC.save();
    }
    *///?}
}
