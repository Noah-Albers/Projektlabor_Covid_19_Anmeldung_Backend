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
					

	// Copy-Paste generated. Just change the class name
	// Automatically grabs and stores all attributes from the class to easily serialize and deserialize those
	private static Map<String, Field> ATTRIBUTES = getAttributes(ContactInfoEntity.class);
	public static final String[] ATTRIBUTE_LIST = getAttributeNames(ContactInfoEntity.class);
	
	@Override
	protected Map<String, Field> attributes() {
		return ATTRIBUTES;
	}
	
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
}
