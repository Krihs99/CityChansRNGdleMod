package com.krihcity.citychanrngdle.fate;

import java.util.List;
import java.util.Random;

public enum FateTier {
    NEUTRAL(0.41f, 0xFFFFFF,
        "The world asks nothing of you today.",
        "the queue is not popping...",
        "Fate shrugs.",
        "Unremarkable, but entirely yours.",
        "just chilling lowkey"
    ),
    GOOD(0.25f, 0x55FF55,
        "Fortune smiles upon you today.",
        "The wind blows in your favor.",
        "Today belongs to you.",
        "yoo? kinda W?"
    ),
    BAD(0.20f, 0xFF6600,
        "Today will test your patience.",
        "You opened global chat.",
        "unlucky bro unlucky",
        "Not your day.",
        "Yep. We lost.",
        "Could've been way worse honestly"
    ),
    GODTIER(0.06f, 0xFFD700,
        "The stars align in your favor.",
        "chilly ice, frosty ice",
        "Kyr is on your team.",
        "bankai baimo",
        "Swissnova City SMP1 island"
    ),
    HELL(0.05f, 0xAA0000,
        "Fate has chosen to make an example of you.",
        "goodbye.",
        "go next",
        "Astraha nether portal",
        "FF",
        "You popped against Grustlers.",
        "Shoveler.",
        "KEKII"
    ),
    MAHJONG(0.03f, 0xAA00AA),
    FUNNY_NUMBERS(0.03f, 0xFF69B4);

    // Tiers shown during the spin animation (MAHJONG is excluded — it's the landing surprise)
    public static final FateTier[] SPIN_CYCLE = { NEUTRAL, GOOD, BAD, GODTIER, HELL };

    public final float weight;
    public final List<String> flavorTexts;
    public final int color;

    FateTier(float weight, int color, String... texts) {
        this.weight = weight;
        this.color = color;
        this.flavorTexts = List.of(texts);
    }

    public String pickFlavorText(Random rng) {
        if (flavorTexts.isEmpty()) return "";
        return flavorTexts.get(rng.nextInt(flavorTexts.size()));
    }
}
