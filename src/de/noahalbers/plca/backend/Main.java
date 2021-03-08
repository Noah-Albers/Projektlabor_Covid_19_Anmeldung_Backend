package de.noahalbers.plca.backend;

import java.io.IOException;

public class Main {
	
	public static void main(String[] args) throws Exception {
		new Main();
	}
	
	public Main() {
		// Loads the config
		try {
			// TODO: Handle exception
			Config.getInstance()
				.register("token", null)
				.register("botname", null)
				.loadConfig();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
	}
}
