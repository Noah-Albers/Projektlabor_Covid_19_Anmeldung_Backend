package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.util.Map;

import de.noahalbers.plca.backend.database.EntityInfo;

public class SimpleUserEntity extends Entity{

	public static final String
	ID = "id",
	FIRSTNAME = "firstname",
	LASTNAME = "lastname";

	// Holds all entrys
	private static Map<String, Field> DB_ENTRYS = getEntrys(SimpleUserEntity.class, true);
	private static Map<String, Field> JSON_ENTRYS = getEntrys(SimpleUserEntity.class, false);

	// Holds a list with all database entrys. Can be used to load all values from a class
	public static final String[] DB_ENTRY_LIST = DB_ENTRYS.keySet().toArray(new String[DB_ENTRYS.size()]);

	
	@EntityInfo(ID)
	public Integer id;
	@EntityInfo(FIRSTNAME)
	public String firstname;
	@EntityInfo(LASTNAME)
	public String lastname;

	public SimpleUserEntity() {}
	
	public SimpleUserEntity(Integer id, String firstname, String lastname) {
		this.id = id;
		this.firstname = firstname;
		this.lastname = lastname;
	}

	@Override
	protected Map<String, Field> entrys(boolean databaseEntrys) {
		return databaseEntrys?DB_ENTRYS:JSON_ENTRYS;
	}
	
	@Override
	public String toString() {
		return String.format("#%d: %s %s", this.id,this.firstname,this.lastname);
	}
}
