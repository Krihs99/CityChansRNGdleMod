package com.krihcity.citychanrngdle.fate;

public enum FunnyNumber {
    SIXTY_SEVEN        ("67",  "You deserve this. Goodbye.",        1),
    SIXTY_NINE         ("69",  "All femboys from the village want a taste", 3),
    SEVEN_TWENTY_SEVEN ("727", "of course this had to be here",     3),
    FOUR_TWENTY        ("420", "i am calm.",                         3);

    public final String displayName;
    public final String flavorText;
    public final int weight;

    FunnyNumber(String displayName, String flavorText, int weight) {
        this.displayName = displayName;
        this.flavorText = flavorText;
        this.weight = weight;
    }
}
