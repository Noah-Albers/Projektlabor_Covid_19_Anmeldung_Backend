package de.noahalbers.plca.backend.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

	// The level that the logger has (Use the above levels and concat them using | )
	private static int LOG_LEVEL_WRITE,LOG_LEVEL_OUTPUT;

	// The output stream to write to the log-file
	private static OutputStream OPEN_FILE;
	
	// Source that can be logged
	private final Object source;

	public Logger(Object source) {
		this.source = source;
	}

	/**
	 * Starts the logger service (Opens the file etc)
	 * @param logLevelWrite the log level for all logs that shall be written to the log-file
	 * @param logLevelOutput the log level for all logs that shall be displayed on the console
	 * @throws IOException 
	 */
	public static void init(File logDirectory,int logLevelWrite,int logLevelOutput) throws IOException {
		LOG_LEVEL_WRITE = logLevelWrite;
		LOG_LEVEL_OUTPUT = logLevelOutput;
		
		// Ensures that the directory exists
		logDirectory.mkdirs();
		
		// Gets the file-formatter
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
		
		// Gets the file
		File file = new File(logDirectory,"Log-"+dtf.format(LocalDateTime.now())+".log");

		// Creates the file
		file.createNewFile();
		
		// Opens the file-stream
		OPEN_FILE = new FileOutputStream(file);
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
	private static void log(int level, Object prefix, Object source, Object msg) {
		// Generates the final message
		String finalMessage = String.format(
				"%s%s%s\n",
				prefix,
				source == null ? "" : (" [" + source.toString() + "]"),
						msg == null ? "" : (" " + msg.toString())
				);

		// Checks if the log-level for output matches
		if ((LOG_LEVEL_OUTPUT & level) != 0) {
			// Outputs the info
			System.out.print(finalMessage);			
		}
		
		// Checks if the log-level for file-writing matches
		if((LOG_LEVEL_WRITE & level) != 0) {
			try {
				// Outputs the message
				OPEN_FILE.write(finalMessage.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				// Error, can likely not be handled
				System.err.println("ERROR");
				e.printStackTrace();
			}
		}
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
