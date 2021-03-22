package de.noahalbers.plca.backend.database.exceptions;

import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

public class DuplicatedEntryException extends SQLException{
	private static final long serialVersionUID = 1736573624006882409L;

	// The combination of entrys that can be duplicated
	private Entry<String/*Name*/,Object/*Value*/>[] entrys;
	
	public DuplicatedEntryException(String name,Object value) {
		this(new SimpleEntry<>(name,value));
	}
	
	@SafeVarargs
	public DuplicatedEntryException(Entry<String, Object>... entrys) {
		this.entrys=entrys;
	}
	
	public Entry<String, Object>[] getEntrys() {
		return this.entrys;
	}
	
}
