package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.sql.Date;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.EntityInfo;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.util.Nullable;

public class UserEntity extends SimpleUserEntity {
	
	public static final String
	POSTAL_CODE = "postalcode",
	LOCATION = "location",
	STREET = "street",
	HOUSE_NUMBER = "housenumber",
	TELEPHONE = "telephone",
	EMAIL = "email",
	RFID = "rfidcode",
	AUTODELETE = "autodeleteaccount",
	REGISTER_DATE = "createdate";
	
	// Copy-Paste generated. Just change the class name
	// Automatically grabs and stores all attributes from the class to easily serialize and deserialize those
	private static Map<String, Field> ATTRIBUTES = getAttributes(UserEntity.class);
	public static final String[] ATTRIBUTE_LIST = getAttributeNames(UserEntity.class);
	public static final String[] OPTIONAL_ATTRIBUTE_LIST = getAttributeNames(UserEntity.class, true);
	public static final String[] REQUIRED_ATTRIBUTE_LIST = getAttributeNames(UserEntity.class, false);
	
	@Override
	protected Map<String, Field> attributes() {
		return ATTRIBUTES;
	}
	
	@EntityInfo(POSTAL_CODE)
	public Integer postalCode;
	@EntityInfo(LOCATION)
	public String location;
	@EntityInfo(STREET)
	public String street;
	@EntityInfo(HOUSE_NUMBER)
	public String housenumber;
	@EntityInfo(TELEPHONE)
	@Nullable
	public String telephonenumber;
	@EntityInfo(EMAIL)
	@Nullable
	public String email;
	@EntityInfo(RFID)
	@Nullable
	public String rfidcode;
	@EntityInfo(AUTODELETE)
	public Boolean autodeleteaccount;
	@EntityInfo(REGISTER_DATE)
	public Date createdate;

	public UserEntity() {}

	public UserEntity(Integer id, String firstname, String lastname,Integer postalCode, String location, String street, String housenumber, Boolean autodeleteaccount, Date createdate) {
		super(id, firstname, lastname);
		this.postalCode = postalCode;
		this.location = location;
		this.street = street;
		this.housenumber = housenumber;
		this.autodeleteaccount = autodeleteaccount;
		this.createdate = createdate;
	}

	@Override
	public String toString() {
		try {
			// Parses the user into an json object
			JSONObject jo = new JSONObject();
			this.save(jo, UserEntity.ATTRIBUTE_LIST);
			
			// Mapps all values into a display list
			return jo.keySet().stream().map(i->i+"='"+jo.get(i)+"'").collect(Collectors.joining(" "));
		} catch (EntitySaveException e) {
			return "User(id."+this.id+")";
		}
	}
}
