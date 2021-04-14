package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Map;

import de.noahalbers.plca.backend.util.Nullable;

import de.noahalbers.plca.backend.database.EntityInfo;

public class TimespentEntity extends Entity {

	public static final String
	ID = "id",
	START_TIME = "start",
	STOP_TIME = "stop",
	END_DISCONNECTED = "enddisconnect",
	USER_ID = "userid";

	// Holds all entrys
	private static Map<String, Field> DB_ENTRYS = getEntrys(TimespentEntity.class, true);
	private static Map<String, Field> JSON_ENTRYS = getEntrys(TimespentEntity.class, false);

	// Holds a list with all database entrys. Can be used to load all values from a class
	public static final String[] DB_ENTRY_LIST = DB_ENTRYS.keySet().toArray(new String[DB_ENTRYS.size()]);
	
	@EntityInfo(ID)
	public Integer id;

	@EntityInfo(START_TIME)
	public Timestamp startTime;
	
	@EntityInfo(STOP_TIME)
	@Nullable
	public Timestamp stopTime;
	
	@EntityInfo(END_DISCONNECTED)
	public boolean gotDisconnected;
	
	@EntityInfo(USER_ID)
	public Integer userId;
	
	public TimespentEntity() {}
	
	@Override
	protected Map<String, Field> entrys(boolean databaseEntrys) {
		return databaseEntrys?DB_ENTRYS:JSON_ENTRYS;
	}
}
