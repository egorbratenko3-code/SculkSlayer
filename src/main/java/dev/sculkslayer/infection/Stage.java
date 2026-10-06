package dev.sculkslayer.infection;

/** Static balance table. Stage index: 0-7 = stages 0..7, 8-10 = Critical 1..3. */
public final class Stage {
	public static final int C1 = 8;
	public static final int C2 = 9;
	public static final int C3 = 10;

	/** Infection percent at which each stage starts. */
	private static final double[] THRESHOLD = {0, 6, 12, 19, 27, 36, 46, 56, 68, 80, 92};
	private static final int[] KILL_ATTEMPTS = {3, 5, 8, 11, 15, 15, 15, 15, 15, 15, 15};
	private static final int[] KILL_RADIUS = {2, 3, 3, 4, 5, 5, 5, 5, 5, 5, 5};
	/** Natural spread steps per second (fine, block-by-block growth near loaded/seeded ground). Ramps up hard from stage 4. */
	private static final double[] SPREAD_PER_SEC = {1, 2, 3, 6, 16, 24, 34, 46, 60, 60, 60};

	// ---- macro territory (the infection front) --------------------------------------------
	// The plague also grows as a circular "front" centered on the monolith, independent of kills.
	// Chunks are only ever touched once they actually load, so an unvisited 5000x5000 area costs nothing.
	// Radius is capped per stage, so reaching the far edge of the plague requires reaching a late stage.
	private static final int[] RADIUS_CAP = {100, 250, 450, 700, 1200, 1500, 1800, 2100, 2300, 2450, 2500};
	/** Blocks/second the front grows while under its stage cap. */
	private static final double[] RADIUS_SPEED = {0.12, 0.12, 0.11, 0.10, 0.09, 0.075, 0.06, 0.04, 0.025, 0.012, 0.006};
	public static final int MAX_RADIUS = RADIUS_CAP[10];
	private static final double[] CRYSTAL_CHANCE = {0, 0, 0, 0.06, 0.18, 0.26, 0.38, 0.48, 0.58, 0.58, 0.58};
	private static final double[] MOB_INFECT_CHANCE = {0, 0, 0, 0, 0.02, 0.06, 0.10, 0.14, 0.19, 0.24, 0.28};
	// ---- progression economy -------------------------------------------------------------
	// Every source of infection is divided by resistance(stage), which grows exponentially:
	// stage 0 -> 1 takes ~10 minutes, but each later stage is ~1.6x harder than the previous one.
	private static final double RESISTANCE_GROWTH = 1.6;
	/** Percent per second from time alone (before resistance). */
	private static final double TIME_BASE = 0.007;
	/** Percent per unholy kill (before resistance). */
	private static final double KILL_BASE = 0.20;
	/** Percent per freshly infected block (before resistance). */
	private static final double BLOCK_BASE = 0.0005;
	/** Percent removed per cleansed block. NOT divided by resistance, so cleansing always matters. */
	public static final double PURGE_LOSS = 0.0003;

	public static int of(double infection) {
		int s = 0;
		for (int i = 0; i < THRESHOLD.length; i++) {
			if (infection >= THRESHOLD[i]) {
				s = i;
			}
		}
		return s;
	}

	public static double threshold(int stage) { return THRESHOLD[clamp(stage)]; }

	public static String label(int stage) {
		if (stage <= 7) return String.valueOf(stage);
		return "critical " + (stage - 7);
	}

	public static int killAttempts(int s) { return KILL_ATTEMPTS[clamp(s)]; }
	public static int killRadius(int s) { return KILL_RADIUS[clamp(s)]; }
	public static double spreadPerTick(int s) { return SPREAD_PER_SEC[clamp(s)] / 20.0; }
	public static double crystalChance(int s) { return CRYSTAL_CHANCE[clamp(s)]; }
	public static double mobInfectChance(int s) { return MOB_INFECT_CHANCE[clamp(s)]; }
	public static double resistance(int s) { return Math.pow(RESISTANCE_GROWTH, clamp(s)); }
	public static double timeGain(int s) { return TIME_BASE / resistance(s); }
	public static double killGain(int s) { return KILL_BASE / resistance(s); }
	public static double blockGain(int s) { return BLOCK_BASE / resistance(s); }
	public static int radiusCap(int s) { return RADIUS_CAP[clamp(s)]; }
	public static double radiusSpeed(int s) { return RADIUS_SPEED[clamp(s)]; }

	/** Multiplier applied to health / damage of infected mobs. */
	public static double mobPower(int s) { return 1.0 + 0.16 * Math.max(0, s - 4); }

	private static int clamp(int s) { return Math.max(0, Math.min(10, s)); }

	private Stage() {}
}
