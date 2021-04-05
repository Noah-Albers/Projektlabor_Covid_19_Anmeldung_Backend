package de.noahalbers.plca.backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.noahalbers.plca.backend.logger.Logger;

public class Config {
	
	// Reference to the logger
	private Logger log = PLCA.getInstance().getLogger();
	
	// Location of the config file
	private static final File CONFIG_FILE = new File("config.cfg");
	
	// Contains all loaded config values
	private Map<String,String> loadedConfig = new HashMap<>();
	
	/**
	 * Saves the currently settings to the config-file
	 * @throws IOException when the progam fails to write to the config file
	 */
	public Config saveConfig() throws IOException{
		
		this.log.debug("Saving config...");
		
		this.log.debug("Config: Opening file");
		
		// Opens the file
		BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG_FILE));
		
		// Gets all entrys
		List<Entry<String, String>> entrys = this.loadedConfig.entrySet().stream().collect(Collectors.toList());
		
		// Sorts the entrys
		Collections.sort(entrys, (a,b)->a.getKey().compareTo(b.getKey()));
		

		this.log.debug("Config: Writing values");
		
		// Saves all values
		for(Entry<String, String> entry : entrys)
			bw.write(entry.getKey()+": "+entry.getValue()+"\n");
		
		this.log.debug("Config: Closing file");
		
		// Closes the writer
		bw.close();
		return this;
	}
	
	/**
	 * Loads the config file
	 * @throws IOException when the config fails to create or could not be loaded
	 */
	public Config loadConfig() throws IOException {
		
		this.log.debug("Config loading");
		
		// Ensures that the config exists
		if(!CONFIG_FILE.exists()) {
			this.log.debug("Config: File does not exists, creating new one");

			// Ensures that the file directory exists
			if(CONFIG_FILE.getParentFile() != null && CONFIG_FILE.getParentFile().isDirectory())
				CONFIG_FILE.getParentFile().mkdirs();

			// Creates the new file
			CONFIG_FILE.createNewFile();
			// Writes the default values
			return this.saveConfig();
		}

		this.log.debug("Config: Opening reader");
		
		// Opens the reader
		BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE));
		
		// Temp-variable to load all lines
		String ln;

		this.log.debug("Config: Scanns all lines");
		
		// Iterates over all lines to load them
		while((ln=br.readLine())!=null) {
			// Gets the key-value split index
			int splitIndex = ln.indexOf(":");
			
			// Checks if the value is valid
			if(ln.startsWith("#") || splitIndex == -1)
				continue;
			
			// Gets the key
			String key = ln.substring(0,splitIndex).trim();
			
			// Checks the config does not contain the key
			if(!this.loadedConfig.containsKey(key))
				continue;
			
			// Adds the loaded value
			this.loadedConfig.put(key, ln.substring(splitIndex+1).trim());
		}
		
		this.log.debug("Config: Closing file");
		
		// Closes the file
		br.close();
		return this.saveConfig();
	}
	
	/**
	 * Saves a default value to the settings
	 * @param key the key of the entry
	 * @param value the default value for the entry
	 */
	public Config register(String key,String value) {
		this.loadedConfig.putIfAbsent(key, value);
		return this;
	}
	
	/**
	 * Returns the saved settings value (Default or loaded)
	 */
	public String get(String key) {
		return this.loadedConfig.getOrDefault(key, null);
	}
	
	
}
