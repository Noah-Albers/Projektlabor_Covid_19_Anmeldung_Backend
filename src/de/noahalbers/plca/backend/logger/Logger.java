package de.noahalbers.plca.backend.logger;

import javax.annotation.Nullable;

public class Logger {

	// All possible log levels
	// Shall be concatenated using | to log both levels
	public static final int NONE = 0, // Nothing will be logged
			DEBUG = 0b1, // General debug messages shall be logged
			DEBUG_SPECIAL = 0b10, // Debug message for the current code that gets debugged, will be logged
			INFO = 0b100, // User-info will get logged
			WARNING = 0b1000, // Warnings will get logged
			ERROR = 0b10000, // Critical errors will be logged
			CRITICAL = 0b100000, // Logs critical information (Person user stuff etc.) SHOULD ONLY BE USED FOR
									// DEBUGGING AND NEVER TURNED ON BY DEFAULT!
			ALL = ~0; // All of the above will be logged

	// The level that the logger has (Used the above levels and concat them using |
	// )
	private static int LOG_LEVEL;

	// Source that can be logged
	@Nullable
	private final Object source;

	public Logger(@Nullable Object source) {
		this.source = source;
	}

	public static void setLogLevel(int level) {
		LOG_LEVEL = level;
	}

	/**
	 * The actual method to log an occurrence. Used by all others to log.
	 * 
	 * @param level
	 *            the level that gets logged (Used to verify, that the message
	 *            should be outputted)
	 * @param prefix
	 *            the prefix that can be printed before the message (Visual
	 *            distinction)
	 * @param msg
	 *            the actual message that shall be logged
	 * @param source
	 *            the source object that created the logger. Used to log from which
	 *            object the logger gets called
	 */
	private static void log(int level, Object prefix, @Nullable Object source, @Nullable Object msg) {
		// Checks if the log-level does not match
		if ((LOG_LEVEL & level) == 0)
			return;

		// Generates the final message
		String finalMessage = String.format(
			"%s%s%s",
			prefix,
			source == null ? "" : (" [" + source.toString() + "]"),
			msg == null ? "" : (" " + msg.toString())
		);

		// Outputs the info
		System.out.println(finalMessage);
	}

	public Logger debug(Object msg) {
		log(DEBUG, "\t[DEBUG]", this.source, msg);
		return this;
	}

	public Logger debug_special(Object msg) {
		log(DEBUG_SPECIAL, "[DEBUG++]", this.source, msg);
		return this;
	}

	public Logger info(Object msg) {
		log(INFO, "[INFO]", this.source, msg);
		return this;
	}

	public Logger warn(Object msg) {
		log(WARNING, "[WARNING]", this.source, msg);
		return this;
	}

	public Logger error(Object msg) {
		log(ERROR, "[ERROR]", this.source, msg);
		return this;
	}

	public Logger critical(Object msg) {
		log(CRITICAL, "[CRITICAL]", this.source, msg);
		return this;
	}

}
