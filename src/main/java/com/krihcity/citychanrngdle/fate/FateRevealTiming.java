package com.krihcity.citychanrngdle.fate;

public final class FateRevealTiming {
    public static final long SPIN_MS              = 6000L;
    public static final long HAND_LEAD_MS         = 1200L;
    public static final long EFFECT_INTERVAL_MS   = 700L;
    public static final long HOLD_MS              = 5000L;

    // Suspense event timings
    public static final long FALSE_LAND_HOLD_MS   = 1100L; // how long the false tier lingers
    public static final long SILENT_SNAP_HOLD_MS  =  900L; // fake hold before silent cut to real tier
    public static final long QUICK_TICK_EXTRA_MS  =  380L; // single-tick flash before real land
    public static final long SLOW_CRAWL_EXTRA_MS  = 2000L; // extra time for the 2 slow ticks
    public static final long DEATH_CRAWL_EXTRA_MS = 1000L; // extra time for the 1 slow tick

    private FateRevealTiming() {}
}
