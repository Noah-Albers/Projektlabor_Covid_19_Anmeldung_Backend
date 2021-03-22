package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.EntityInfo;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;

public class UserEntity extends SimpleUserEntity {

	// Holds all entrys
	private static Map<String, Field> DB_ENTRYS = getEntrys(UserEntity.class, true);
	private static Map<String, Field> JSON_ENTRYS = getEntrys(UserEntity.class, false);

	// Holds a list with all database entrys. Can be used to load all values from a class
	public static final String[] DB_ENTRY_LIST = DB_ENTRYS.keySet().toArray(new String[DB_ENTRYS.size()]);
	
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
	public Timestamp createdate;

	public UserEntity() {}

	public UserEntity(Integer id, String firstname, String lastname,Integer postalCode, String location, String street, String housenumber, Boolean autodeleteaccount, Timestamp createdate) {
		super(id, firstname, lastname);
		this.postalCode = postalCode;
		this.location = location;
		this.street = street;
		this.housenumber = housenumber;
		this.autodeleteaccount = autodeleteaccount;
		this.createdate = createdate;
	}

	@Override
	protected Map<String, Field> entrys(boolean databaseEntrys) {
		return databaseEntrys?DB_ENTRYS:JSON_ENTRYS;
	}
	
	@Override
	public String toString() {
		try {
			// Parses the user into an json object
			JSONObject jo = new JSONObject();
			this.save(jo, DB_ENTRY_LIST);
			
			// Mapps all values into a display list
			return jo.keySet().stream().map(i->i+"='"+jo.get(i)+"'").collect(Collectors.joining(" "));
		} catch (EntitySaveException e) {
			return "User(id."+this.id+")";
		}
	}
}
