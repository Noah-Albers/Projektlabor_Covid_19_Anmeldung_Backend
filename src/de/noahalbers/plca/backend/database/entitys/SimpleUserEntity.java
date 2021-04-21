package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.util.Map;

import de.noahalbers.plca.backend.database.EntityInfo;

public class SimpleUserEntity extends Entity{

	public static final String
	ID = "id",
	FIRSTNAME = "firstname",
	LASTNAME = "lastname";

	// Copy-Paste generated. Just change the class name
	// Automatically grabs and stores all attributes from the class to easily serialize and deserialize those
	private static Map<String, Field> ATTRIBUTES = getAttributes(SimpleUserEntity.class);
	public static final String[] ATTRIBUTE_LIST = getAttributeNames(SimpleUserEntity.class);
	
	@Override
	protected Map<String, Field> attributes() {
		return ATTRIBUTES;
	}

	
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
	public String toString() {
		return String.format("#%d: %s %s", this.id,this.firstname,this.lastname);
	}
}
