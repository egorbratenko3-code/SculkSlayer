package dev.sculkslayer.client;

/** Latest plague data received from the server. */
public final class HudData {
	public static boolean active;
	public static boolean cleansed;
	public static String infected = "0.0";
	public static String stage = "0";
	public static String monolith = "?";
	public static String core = "";

	/** Called when leaving a world so the HUD never shows stale numbers from the previous one. */
	public static void reset() {
		active = false;
		cleansed = false;
		infected = "0.0";
		stage = "0";
		monolith = "?";
		core = "";
	}

	public static void parse(String data) {
		try {
			String[] p = data.split("\\|", -1);
			active = "1".equals(p[0]);
			infected = p[1];
			stage = p[2];
			monolith = p[3].replace(',', ' ');
			core = p.length > 4 ? p[4].replace(',', ' ') : "";
			cleansed = p.length > 5 && "1".equals(p[5]);
		} catch (RuntimeException e) {
			active = false;
		}
	}

	private HudData() {}
}
