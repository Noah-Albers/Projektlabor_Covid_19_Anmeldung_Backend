package de.noahalbers.plca.backend.util;

public class PresetTimer extends Timer {

	// Preset time that can be tested
	private long presetTime;

	public PresetTimer(long presetTime) {
		this.presetTime = presetTime;
	}

	/**
	 * @return if the timer has reached the preset time.
	 */
	public boolean hasReached() {
		return this.hasReached(this.presetTime);
	}

	/**
	 * Checks if the timer has reached the preset time. If so, it resets the timer
	 * and returns true
	 */
	public boolean hasReachedIfReset() {
		return this.hasReachedIfReset(this.presetTime);
	}

}
