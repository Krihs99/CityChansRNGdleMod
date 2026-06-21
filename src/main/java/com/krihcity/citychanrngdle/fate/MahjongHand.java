package com.krihcity.citychanrngdle.fate;

public enum MahjongHand {
    NINE_GATES          ("Nine Gates",             "The heavens open their gates for you."),
    THIRTEEN_ORPHANS    ("Thirteen Orphans",        "Chaos claims you as its own."),
    UNDER_THE_SEA       ("Under the Sea",           "The ocean calls your name."),
    ALL_TERMINALS       ("All Terminals",           "You see everything and nothing."),
    TSUMO               ("Tsumo",                  "A self-drawn victory."),
    FOUR_CONCEALED_TRIPLETS("Four Concealed Triplets", "Power, concealed and absolute."),
    ALL_GREEN_IMPERIAL_JADE("All Green Imperial Jade", "Blessed by the jade emperor."),
    RIICHI              ("Riichi",                 "You have declared your hand."),
    CHANTA              ("Chanta",                 "Drift, float, and wander."),
    MIXED_TRIPLE_SEQUENCE("Mixed Triple Sequence", "All paths converge on you.");

    public final String displayName;
    public final String flavorText;

    MahjongHand(String displayName, String flavorText) {
        this.displayName = displayName;
        this.flavorText = flavorText;
    }
}
