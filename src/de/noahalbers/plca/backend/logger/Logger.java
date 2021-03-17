package de.noahalbers.plca.backend.logger;

public class Logger {

	// All possible log levels
	// Shall be concatenated using | to log both levels
	public static int
	NONE = 0,				// Nothing will be logged
	DEBUG = 0b1,			// General debug messages shall be logged
	DEBUG_SPECIAL = 0b10,	// Debug message for the current code that gets debugged, will be logged
	INFO = 0b100,			// User-info will get logged
	WARNING = 0b1000,		// Warnings will get logged
	ERROR = 0b10000,		// Critical errors will be logged
	ALL = ~0;				// All of the above will be logged
	
	// The level that the logger has (Used the above levels and concat them using | )
	private int logLevel;
	
	public Logger(int logLevel) {
		this.logLevel = logLevel;
	}
	
	/**
	 * The actual method to log an occurrence.
	 * Used by all others to log.
	 * 
	 * @param level the level that gets logged (Used to verify, that the message should be outputted)
	 * @param prefix the prefix that can be printed before the message (Visual distinction)
	 * @param msg the actual message that shall be logged
	 */
	private void log(int level,String prefix,Object msg) {
		// Checks if the log-level does not match
		if((this.logLevel & level) == 0)
			return;
		
		// Outputs the info
		System.out.println(prefix+msg.toString());
	}
	
	public void debug(Object msg) {
		this.log(DEBUG, "\t[DEBUG] ", msg);
	}
	
	public void debug_special(Object msg) {
		this.log(DEBUG_SPECIAL, "[DEBUG++] ", msg);
	}

	public void info(Object msg) {
		this.log(INFO, "[INFO] ", msg);
	}
	public void warn(Object msg) {
		this.log(WARNING, "[WARNING] ", msg);
	}
	public void error(Object msg) {
		this.log(ERROR, "[ERROR] ", msg);
	}
	
}
