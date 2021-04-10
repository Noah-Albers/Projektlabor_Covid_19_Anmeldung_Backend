package de.noahalbers.plca.backend;

import de.noahalbers.plca.backend.logger.Logger;

public class Main {
	
	public static void main(String[] args) throws Exception {
		// Sets the log level
		Logger.setLogLevel(Logger.ALL);
				
		PLCA.getInstance().init();
	}
}
