package de.noahalbers.plca.backend.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.noahalbers.plca.backend.config.loaders.BaseValue;
import de.noahalbers.plca.backend.logger.Logger;
import de.noahalbers.plca.backend.util.Nullable;

public class Config {
	
	// Reference to the logger
	private Logger log = new Logger("Config");
	
	// Location of the config file
	private static final File CONFIG_FILE = new File("config.cfg");
	
	// Contains all config values
	private Map<String,Entry<BaseValue<?>, String>> loadedConfig = new HashMap<>();
	
	/**
	 * Saves the currently settings to the config-file
	 * @throws IOException when the progam fails to write to the config file
	 */
	public Config saveConfig() throws IOException{
		
		this.log
		.debug("Saving config...")
		.debug("Opening file");
		
		// Opens the file
		BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG_FILE));
		
		// Gets all entrys
		List<Entry<String, Entry<BaseValue<?>, String>>> entrys = this.loadedConfig.entrySet().stream().collect(Collectors.toList());
		
		// Sorts the entrys
		Collections.sort(entrys, (a,b)->a.getKey().compareTo(b.getKey()));
		
		this.log.debug("Writing values");
		
		// Saves all values
		for(Entry<String, Entry<BaseValue<?>, String>> entry : entrys) {
			// Checks if a comment has been specified
			if(entry.getValue().getValue() != null)
				// Writes the comment
				bw.write("# "+entry.getValue().getValue()+"\n");
			// Writes the value
			bw.write(entry.getKey()+": "+entry.getValue().getKey().saveValue()+"\n");
		}
		
		this.log.debug("Closing file");
		
		// Closes the writer
		bw.close();
		return this;
	}
	
	/**
	 * Loads the config file
	 * @throws IOException when the config fails to create or could not be loaded
	 */
	@SuppressWarnings("resource")
	public Config loadConfig() throws IOException,ConfigLoadException {
		
		this.log.debug("Config loading");
		
		// Ensures that the config exists
		if(!CONFIG_FILE.exists()) {
			this.log.debug("File does not exists, creating new one");

			// Ensures that the file directory exists
			if(CONFIG_FILE.getParentFile() != null && CONFIG_FILE.getParentFile().isDirectory())
				CONFIG_FILE.getParentFile().mkdirs();

			// Creates the new file
			CONFIG_FILE.createNewFile();
			// Writes the default values
			this.saveConfig();
		}

		this.log.debug("Opening reader");
		
		// Opens the reader
		BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE));
		
		// Temp-variable to load all lines
		String ln;

		this.log.debug("Scanns all lines");
		
		// Stores all scanned lines
		Map<String,String> lines = new HashMap<String,String>();

		// Iterates over all lines to load them
		while((ln=br.readLine())!=null) {
			// Gets the key-value split index
			int splitIndex = ln.indexOf(":");
			
			// Checks if the value is valid
			if(ln.startsWith("#") || splitIndex == -1)
				continue;
			
			// Appends the line
			lines.put(ln.substring(0,splitIndex).trim(), ln.substring(splitIndex+1).trim());
		}
		
		// Will be set to an attribute if there was an error loading it
		String error = null;
		
		// Tries to load all values
		for(Entry<String, Entry<BaseValue<?>, String>> set : this.loadedConfig.entrySet()) {
			
			// Gets the settings from the file
			String rawSet = lines.getOrDefault(set.getKey(), null);
			
			// Tries to load the value
			if(rawSet == null || !set.getValue().getKey().loadObject(rawSet))
				error=set.getKey();
		}
		
		// Checks if there was an error with any attribute
		if(error != null) {
			// Saves the config
			this.saveConfig();
			
			// Throws an error
			throw new ConfigLoadException(error);
		}
		
		this.log.debug("Closing file");
		
		// Closes the file
		br.close();
		return this.saveConfig();
	}
	
	/**
	 * Saves a default value to the settings
	 * @param key the key of the entry
	 * @param value the default value of that config-attribute
	 * @param optComment optionally an comment that will be displayed inside the config when saved
	 */
	public Config register(String key,BaseValue<?> value,@Nullable String optComment) {
		this.loadedConfig.putIfAbsent(key, new AbstractMap.SimpleEntry<BaseValue<?>,String>(value,optComment));
		return this;
	}
	
	public Config register(String key,BaseValue<?> value) {
		return this.register(key, value,null);
	}
	
	/**
	 * Returns the saved settings value (Default or loaded)
	 */
	public Object get(String key) {
		// Gets the basevalue (Loader)
		Entry<BaseValue<?>,String> val = this.loadedConfig.getOrDefault(key, null);
		
		// Returns the value of null if absent
		return val == null ? null : val.getKey().value;
	}
	
	/**
	 * Returns the saved settings value (Default or loaded)
	 * This method uses generics to get the object without casting. The program must be sure that this value will exist at the final program, otherwise this method will cause an exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T getUnsafe(String key) {
		return (T) this.get(key);
	}
	
	
}
