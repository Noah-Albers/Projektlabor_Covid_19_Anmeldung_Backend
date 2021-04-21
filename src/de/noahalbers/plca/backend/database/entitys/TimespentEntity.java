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

	// Copy-Paste generated. Just change the class name
	// Automatically grabs and stores all attributes from the class to easily serialize and deserialize those
	private static Map<String, Field> ATTRIBUTES = getAttributes(TimespentEntity.class);
	public static final String[] ATTRIBUTE_LIST = getAttributeNames(TimespentEntity.class);
	public static final String[] OPTIONAL_ATTRIBUTE_LIST = getAttributeNames(TimespentEntity.class, true);
	public static final String[] REQUIRED_ATTRIBUTE_LIST = getAttributeNames(TimespentEntity.class, false);
	
	@Override
	protected Map<String, Field> attributes() {
		return ATTRIBUTES;
	}
	
	@EntityInfo(ID)
	public Integer id;

	@EntityInfo(START_TIME)
	public Timestamp startTime;
	
	@EntityInfo(STOP_TIME)
	@Nullable
	public Timestamp stopTime;
	
	@EntityInfo(END_DISCONNECTED)
	public Boolean gotDisconnected;
	
	@EntityInfo(USER_ID)
	public Integer userId;
	
	public TimespentEntity() {}
}
