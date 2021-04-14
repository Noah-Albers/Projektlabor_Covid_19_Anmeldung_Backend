package de.noahalbers.plca.backend;

import java.io.File;
import java.io.IOException;

import de.noahalbers.plca.backend.logger.Logger;

public class Main {
	
	public static void main(String[] args) {
		
		// If the debug parameter is set
		int debug = args.length > 0 && args[0].equalsIgnoreCase("-d")? (Logger.CRITICAL | Logger.DEBUG) : Logger.NONE;
		
		// Starts the logger
		try {
			Logger.init(
				new File(debug == Logger.NONE ? "logs/" : "criticallogs/"),
				Logger.DEBUG | Logger.INFO | Logger.WARNING | Logger.ERROR | debug,
				Logger.INFO | Logger.WARNING | Logger.ERROR | debug
			);
		} catch (IOException e) {
			System.out.println("Failed to start logger: "+e.getMessage());
			return;
		}
				
		PLCA.getInstance().init();
	}
}
