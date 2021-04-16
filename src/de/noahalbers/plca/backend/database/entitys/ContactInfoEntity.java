package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Map;

import de.noahalbers.plca.backend.database.EntityInfo;

public class ContactInfoEntity extends Entity{
	public static final String
	INFECTED_STARTTIME = "istart",
	INFECTED_STOPTIME = "istop",
	CONTACT_STARTTIME = "cstart",
	CONTACT_STOPTIME = "cstop",
	CONTACT_ID = "cid";
					

	// Holds all entrys
	private static Map<String, Field> DB_ENTRYS = getEntrys(ContactInfoEntity.class, true);
	private static Map<String, Field> JSON_ENTRYS = getEntrys(ContactInfoEntity.class, false);
	
	// Holds a list with all database entrys. Can be used to load all values from a class
	public static final String[] DB_ENTRY_LIST = DB_ENTRYS.keySet().toArray(new String[DB_ENTRYS.size()]);

	// Time when the infected person arrived
	@EntityInfo(INFECTED_STARTTIME)
	public Timestamp infectedStarttime;
	
	// Time when the infected person left
	@EntityInfo(INFECTED_STOPTIME)
	public Timestamp infectedEndtime;

	// Time when the contact person arrived
	@EntityInfo(CONTACT_STARTTIME)
	public Timestamp contactStarttime;
	
	// Time when the contact person left
	@EntityInfo(CONTACT_STOPTIME)
	public Timestamp contactEndtime;

	// Id of the contact person
	@EntityInfo(CONTACT_ID)
	public Integer contactID;
	
	@Override
	protected Map<String, Field> entrys(boolean databaseEntrys) {
		return databaseEntrys?DB_ENTRYS:JSON_ENTRYS;
	}
}