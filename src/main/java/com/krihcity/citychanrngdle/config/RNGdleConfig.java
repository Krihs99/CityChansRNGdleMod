package com.krihcity.citychanrngdle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.krihcity.citychanrngdle.CityChanRNGdle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RNGdleConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "citychanrngdle.json";

    public static RNGdleConfig INSTANCE = buildDefault();

    // ── Preset ────────────────────────────────────────────────────────────────

    public static final List<String> VALID_PRESETS = List.of("VANILLA", "DEFAULT", "KRIHCITY");

    private static final Set<String> CUSTOM_EFFECTS = Set.of(
        "ENLIGHTENED", "BOUNTIFUL_HARVEST", "COLORBLIND", "LIGHTWEIGHT",
        "SLIPPERY", "BROKEN_LEGS", "WEEPING_ANGEL", "FRAIL"
    );

    // ── Top-level fields ──────────────────────────────────────────────────────

    public boolean defaultOptedIn = false;
    public String preset = "DEFAULT";
    public Map<String, Integer> tierWeights = new LinkedHashMap<>();
    public Map<String, EffectConfig> effects = new LinkedHashMap<>();
    public Map<String, List<FlavorEntry>> tierFlavors = new LinkedHashMap<>();
    public Map<String, FlavorEntry> mahjongFlavors = new LinkedHashMap<>();
    public Map<String, FlavorEntry> funnyNumberFlavors = new LinkedHashMap<>();

    // ── Inner types ───────────────────────────────────────────────────────────

    public static class EffectConfig {
        public boolean enabled = true;
        public String tier = "NEUTRAL";
        public int weight = 10;
        public int amplifier = 0;

        public EffectConfig() {}

        public EffectConfig(boolean enabled, String tier, int weight) {
            this.enabled = enabled;
            this.tier = tier;
            this.weight = weight;
        }
    }

    public static class FlavorEntry {
        public String text = "";
        public boolean enabled = true;
        public transient boolean insideJoke = false;

        public FlavorEntry() {}

        public FlavorEntry(String text, boolean enabled) {
            this.text = text;
            this.enabled = enabled;
        }

        public FlavorEntry(String text, boolean enabled, boolean insideJoke) {
            this.text = text;
            this.enabled = enabled;
            this.insideJoke = insideJoke;
        }
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load(MinecraftServer server) {
        Path path = configPath(server);
        if (!Files.exists(path)) {
            INSTANCE = buildDefault();
            save(server);
            CityChanRNGdle.LOGGER.info("RNGdle config not found — wrote defaults to {}", path);
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            INSTANCE = GSON.fromJson(reader, RNGdleConfig.class);
            mergeDefaults();
            applyInsideJokeFlags();
            CityChanRNGdle.LOGGER.info("RNGdle config loaded from {}", path);
        } catch (IOException e) {
            CityChanRNGdle.LOGGER.error("Failed to load RNGdle config, using defaults", e);
            INSTANCE = buildDefault();
        }
    }

    public static void save(MinecraftServer server) {
        Path path = configPath(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            CityChanRNGdle.LOGGER.error("Failed to save RNGdle config", e);
        }
    }

    private static Path configPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                     .resolve("serverconfig")
                     .resolve(FILE_NAME);
    }

    // ── Preset application ────────────────────────────────────────────────────

    public static void applyPreset(String presetName) {
        INSTANCE.preset = presetName;
        // All presets reset to default values — no manual overrides survive.
        RNGdleConfig defaults = buildDefault();
        defaults.tierWeights.forEach((k, v) -> INSTANCE.tierWeights.put(k, v));
        for (Map.Entry<String, EffectConfig> e : INSTANCE.effects.entrySet()) {
            EffectConfig def = defaults.effects.get(e.getKey());
            if (def != null) {
                e.getValue().enabled = def.enabled;
                e.getValue().weight = def.weight;
                e.getValue().amplifier = def.amplifier;
            }
        }
        switch (presetName) {
            case "VANILLA" -> {
                // Default reset already happened — additionally disable custom mod effects
                for (Map.Entry<String, EffectConfig> e : INSTANCE.effects.entrySet()) {
                    if (CUSTOM_EFFECTS.contains(e.getKey())) e.getValue().enabled = false;
                }
                setInsideJokeFlavors(false);
            }
            case "DEFAULT" -> setInsideJokeFlavors(false);
            case "KRIHCITY" -> setInsideJokeFlavors(true);
        }
    }

    private static void mergeDefaults() {
        RNGdleConfig defaults = buildDefault();
        // New tiers or tier weight keys added in code → appear in config with default weight
        defaults.tierWeights.forEach(INSTANCE.tierWeights::putIfAbsent);
        // New effects added in code → appear in config with default settings
        defaults.effects.forEach(INSTANCE.effects::putIfAbsent);
        // New tier flavor categories (e.g. a whole new tier) → added with default list
        defaults.tierFlavors.forEach(INSTANCE.tierFlavors::putIfAbsent);
        // New mahjong hands added in code → appear with default flavor text
        defaults.mahjongFlavors.forEach(INSTANCE.mahjongFlavors::putIfAbsent);
        // New funny numbers added in code → appear with default flavor text
        defaults.funnyNumberFlavors.forEach(INSTANCE.funnyNumberFlavors::putIfAbsent);
    }

    private static void applyInsideJokeFlags() {
        RNGdleConfig defaults = buildDefault();
        // Tier flavors: match entries by text so custom additions are left untouched
        for (Map.Entry<String, List<FlavorEntry>> entry : INSTANCE.tierFlavors.entrySet()) {
            List<FlavorEntry> defaultList = defaults.tierFlavors.get(entry.getKey());
            if (defaultList == null) continue;
            Map<String, Boolean> flagsByText = new HashMap<>();
            for (FlavorEntry def : defaultList) flagsByText.put(def.text, def.insideJoke);
            for (FlavorEntry live : entry.getValue()) {
                Boolean flag = flagsByText.get(live.text);
                if (flag != null) live.insideJoke = flag;
            }
        }
        // Mahjong / funny-number flavors: match by key
        for (Map.Entry<String, FlavorEntry> entry : INSTANCE.mahjongFlavors.entrySet()) {
            FlavorEntry def = defaults.mahjongFlavors.get(entry.getKey());
            if (def != null) entry.getValue().insideJoke = def.insideJoke;
        }
        for (Map.Entry<String, FlavorEntry> entry : INSTANCE.funnyNumberFlavors.entrySet()) {
            FlavorEntry def = defaults.funnyNumberFlavors.get(entry.getKey());
            if (def != null) entry.getValue().insideJoke = def.insideJoke;
        }
    }

    private static void setInsideJokeFlavors(boolean enabled) {
        for (List<FlavorEntry> list : INSTANCE.tierFlavors.values()) {
            for (FlavorEntry e : list) if (e.insideJoke) e.enabled = enabled;
        }
        for (FlavorEntry e : INSTANCE.mahjongFlavors.values()) {
            if (e.insideJoke) e.enabled = enabled;
        }
        for (FlavorEntry e : INSTANCE.funnyNumberFlavors.values()) {
            if (e.insideJoke) e.enabled = enabled;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public List<String> getEnabledFlavorsForTier(String tierName) {
        List<FlavorEntry> entries = tierFlavors.get(tierName);
        if (entries == null) return List.of();
        return entries.stream()
                .filter(e -> e.enabled)
                .map(e -> e.text)
                .toList();
    }

    public String getEnabledMahjongFlavor(String handName) {
        FlavorEntry entry = mahjongFlavors.get(handName);
        if (entry == null || !entry.enabled) return "";
        return entry.text;
    }

    public String getEnabledFunnyNumberFlavor(String numberName) {
        FlavorEntry entry = funnyNumberFlavors.get(numberName);
        if (entry == null || !entry.enabled) return "";
        return entry.text;
    }

    // ── Default values ────────────────────────────────────────────────────────

    public static RNGdleConfig buildDefault() {
        RNGdleConfig c = new RNGdleConfig();

        c.defaultOptedIn = false;
        c.preset = "DEFAULT";

        // Tier weights (integers — normalized at runtime, any scale works)
        c.tierWeights.put("NEUTRAL",       41);
        c.tierWeights.put("GOOD",          25);
        c.tierWeights.put("BAD",           20);
        c.tierWeights.put("GODTIER",        6);
        c.tierWeights.put("HELL",           5);
        c.tierWeights.put("MAHJONG",        3);
        c.tierWeights.put("FUNNY_NUMBERS",  3);

        // Effects
        c.effects.put("DOLPHINS_GRACE",    new EffectConfig(true, "GODTIER",  10));
        c.effects.put("HASTE",             new EffectConfig(true, "GODTIER",  10));
        c.effects.put("RESISTANCE",        new EffectConfig(true, "GODTIER",  10));
        c.effects.put("REGENERATION",      new EffectConfig(true, "GODTIER",  10));
        c.effects.put("ENLIGHTENED",       new EffectConfig(true, "GODTIER",  10));

        c.effects.put("HERO_OF_VILLAGE",   new EffectConfig(true, "GOOD",     10));
        c.effects.put("INVISIBILITY",      new EffectConfig(true, "GOOD",     10));
        c.effects.put("NIGHT_VISION",      new EffectConfig(true, "GOOD",     10));
        c.effects.put("FIRE_RESISTANCE",   new EffectConfig(true, "GOOD",     10));
        c.effects.put("SPEED",             new EffectConfig(true, "GOOD",     10));
        c.effects.put("CONDUIT_POWER",     new EffectConfig(true, "GOOD",     10));
        c.effects.put("STRENGTH",          new EffectConfig(true, "GOOD",     10));
        c.effects.put("BOUNTIFUL_HARVEST", new EffectConfig(true, "GOOD",     10));

        c.effects.put("JUMP_BOOST",        new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("LUCK",              new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("GLOWING",           new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("SLOW_FALLING",      new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("WATER_BREATHING",   new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("UNLUCK",            new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("WEAVING",           new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("WIND_CHARGED",      new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("OOZING",            new EffectConfig(true, "NEUTRAL",  20));
        c.effects.put("COLORBLIND",        new EffectConfig(true, "NEUTRAL",   1));
        c.effects.put("LIGHTWEIGHT",       new EffectConfig(true, "NEUTRAL",  10));

        c.effects.put("HUNGER",            new EffectConfig(true, "BAD",      10));
        c.effects.put("BAD_OMEN",          new EffectConfig(true, "BAD",      10));
        c.effects.put("SLOWNESS",          new EffectConfig(true, "BAD",      10));
        c.effects.put("WEAKNESS",          new EffectConfig(true, "BAD",      10));
        c.effects.put("SLIPPERY",          new EffectConfig(true, "BAD",      10));
        c.effects.put("FRAIL",             new EffectConfig(true, "BAD",      10));

        c.effects.put("MINING_FATIGUE",    new EffectConfig(true, "HELL",     10));
        c.effects.put("INFESTED",          new EffectConfig(true, "HELL",     10));
        c.effects.put("BLINDNESS",         new EffectConfig(true, "HELL",     10));
        c.effects.put("DARKNESS",          new EffectConfig(true, "HELL",     10));
        c.effects.put("BROKEN_LEGS",       new EffectConfig(true, "HELL",     10));
        c.effects.put("WEEPING_ANGEL",     new EffectConfig(true, "HELL",     10));

        // Extra vanilla effects — disabled by default; enable via config commands
        c.effects.put("SATURATION",        new EffectConfig(false, "GODTIER", 10));
        c.effects.put("HEALTH_BOOST",      new EffectConfig(false, "GOOD",    10));
        c.effects.put("ABSORPTION",        new EffectConfig(false, "GOOD",    10));
        c.effects.put("TRIAL_OMEN",        new EffectConfig(false, "BAD",     10));
        c.effects.put("NAUSEA",            new EffectConfig(false, "HELL",    10));
        c.effects.put("POISON",            new EffectConfig(false, "HELL",    10));
        c.effects.put("WITHER",            new EffectConfig(false, "HELL",    10));
        c.effects.put("LEVITATION",        new EffectConfig(false, "HELL",    10));

        // Tier flavor texts — insideJoke=true marks server-specific references
        c.tierFlavors.put("NEUTRAL", new ArrayList<>(List.of(
            new FlavorEntry("The world asks nothing of you today.", true),
            new FlavorEntry("the queue is not popping...",          true, true),
            new FlavorEntry("Fate shrugs.",                         true),
            new FlavorEntry("Unremarkable, but entirely yours.",    true),
            new FlavorEntry("just chilling lowkey",                 true)
        )));
        c.tierFlavors.put("GOOD", new ArrayList<>(List.of(
            new FlavorEntry("Fortune smiles upon you today.", true),
            new FlavorEntry("The wind blows in your favor.",  true),
            new FlavorEntry("Today belongs to you.",          true),
            new FlavorEntry("yoo? kinda W?",                  true)
        )));
        c.tierFlavors.put("BAD", new ArrayList<>(List.of(
            new FlavorEntry("Today will test your patience.",   true),
            new FlavorEntry("You opened global chat.",          true, true),
            new FlavorEntry("unlucky bro unlucky",              true),
            new FlavorEntry("Not your day.",                    true),
            new FlavorEntry("Yep. We lost.",                    true, true),
            new FlavorEntry("Could've been way worse honestly", true)
        )));
        c.tierFlavors.put("GODTIER", new ArrayList<>(List.of(
            new FlavorEntry("The stars align in your favor.", true),
            new FlavorEntry("The universe owes you one.",     true),
            new FlavorEntry("chilly ice, frosty ice",         true, true),
            new FlavorEntry("Kyr is on your team.",           true, true),
            new FlavorEntry("bankai baimo",                   true, true),
            new FlavorEntry("Swissnova City SMP1 island",     true, true)
        )));
        c.tierFlavors.put("HELL", new ArrayList<>(List.of(
            new FlavorEntry("Fate has chosen to make an example of you.", true),
            new FlavorEntry("goodbye.",                                    true),
            new FlavorEntry("go next",                                     true, true),
            new FlavorEntry("Astraha nether portal",                       true, true),
            new FlavorEntry("FF",                                          true, true),
            new FlavorEntry("You popped against Grustlers.",               true, true),
            new FlavorEntry("Shoveler.",                                   true, true),
            new FlavorEntry("KEKII",                                       true, true)
        )));

        // Mahjong flavor texts (none are inside jokes — all are poetic/general)
        c.mahjongFlavors.put("NINE_GATES",              new FlavorEntry("The heavens open their gates for you.", true));
        c.mahjongFlavors.put("THIRTEEN_ORPHANS",        new FlavorEntry("Chaos claims you as its own.",          true));
        c.mahjongFlavors.put("UNDER_THE_SEA",           new FlavorEntry("The ocean calls your name.",            true));
        c.mahjongFlavors.put("ALL_TERMINALS",           new FlavorEntry("You see everything and nothing.",       true));
        c.mahjongFlavors.put("TSUMO",                   new FlavorEntry("A self-drawn victory.",                 true));
        c.mahjongFlavors.put("FOUR_CONCEALED_TRIPLETS", new FlavorEntry("Power, concealed and absolute.",        true));
        c.mahjongFlavors.put("ALL_GREEN_IMPERIAL_JADE", new FlavorEntry("Blessed by the jade emperor.",          true));
        c.mahjongFlavors.put("RIICHI",                  new FlavorEntry("You have declared your hand.",          true));
        c.mahjongFlavors.put("CHANTA",                  new FlavorEntry("Drift, float, and wander.",             true));
        c.mahjongFlavors.put("MIXED_TRIPLE_SEQUENCE",   new FlavorEntry("All paths converge on you.",            true));

        // Funny number flavor texts
        c.funnyNumberFlavors.put("SIXTY_SEVEN",        new FlavorEntry("You deserve this. Goodbye.",                true));
        c.funnyNumberFlavors.put("SIXTY_NINE",         new FlavorEntry("All femboys from the village want a taste", true, true));
        c.funnyNumberFlavors.put("SEVEN_TWENTY_SEVEN", new FlavorEntry("of course this had to be here",             true));
        c.funnyNumberFlavors.put("FOUR_TWENTY",        new FlavorEntry("i am calm.",                                true, true));

        return c;
    }
}
