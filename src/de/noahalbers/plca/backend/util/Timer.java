package de.noahalbers.plca.backend.util;

public class Timer {

	// Last time the timer got reset at (Millis)
	private long lastTime;
	
	public Timer() {
		this.reset();
	}
	
	/**
	 * @return if the given time in millis has already been reached by the timer
	 */
	public boolean hasReached(long time) {
		return System.currentTimeMillis()-this.lastTime >= time;
	}
	
	/**
	 * Resets if the time has already been reached
	 * 
	 * @param time the time in millis that should be reached
	 * @return if the given time in millis has already been reached by the timer
	 */
	public boolean hasReachedIfReset(long time) {
		boolean reached = this.hasReached(time);

		// Checks if the time has already been reached
		if(reached)
			this.reset();
		return reached;
	}
	
	/**
	 * Resets the timer to restart it
	 */
	public void reset() {
		this.lastTime=System.currentTimeMillis();
	}
	
}
