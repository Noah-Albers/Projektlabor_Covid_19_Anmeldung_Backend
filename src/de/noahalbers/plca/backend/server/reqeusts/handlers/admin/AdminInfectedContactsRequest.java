package de.noahalbers.plca.backend.server.reqeusts.handlers.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.ContactInfoEntity;
import de.noahalbers.plca.backend.database.entitys.UserEntity;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionCheck;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionChecks;

public class AdminInfectedContactsRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		afterdate: No afterdate given
	 * 		database: Backend failed to establish a valid database connection
	 * 		user: The user is not given.
	 * 		not_found: the user could not be found.
	 * 		unknown: An unkown error occurred
	 * 		margintime: The margin-time is not given or less than 0
	 * 	
	 * 	Success:
	 * 		users: Array
	 * 			Loadable {@link UserEntity}
	 * 			contactinfo: Array
	 * 				Loadable {@link ContactInfoEntity} for the user
	 * 	
	 * Request:
	 * 	afterdate: the date after which the contacts should be listed.
	 * 	user: the user-id of the user of which the contacts should be listed
	 * 	margintime: how many minutes of margin (spacing) should be counted to a users logout time. Represents the time that the aerosols are still present.
	 * 
	 */
	
	@Override
	public PermissionCheck[] getPermissionChecks() {
		return of(
			PermissionChecks.PERM_ADMIN,
			PermissionChecks.CHECK_ADMIN_AUTH_CODE,
			PermissionChecks.CHECK_ADMIN_NOT_FROZEN
		);
	}

	@Override
	public void execute(Request request) throws IOException {
		// Gets the after-date
		Long afterDateStamp = request.getFromMessage("afterdate", Long.class);
		if(afterDateStamp == null) {
			this.sendErrorMissingField(request, "afterdate");
			return;
		}
		
		// Gets the user id
		Integer userId = request.getFromMessage("user", Integer.class);
		if(userId == null) {
			this.sendErrorMissingField(request, "user");
			return;
		}
		
		// Gets the margin-time
		Integer marginTime = request.getFromMessage("margintime", Integer.class);
		if(marginTime==null || marginTime < 0) {
			this.sendErrorMissingField(request, "margintime");
			return;
		}
		
		try {
			// Checks if the given user exists
			if(!this.database.doesUserExists(request.startDatabaseConnection(), userId)) {
				this.sendErrorMissingField(request, "not_found");
				return;
			}
			
			// Gets the contacts from the user
			Map<UserEntity, List<ContactInfoEntity>> contacts = this.database.getContactInfosForUser(request.startDatabaseConnection(), userId, new Timestamp(afterDateStamp),marginTime);
			
			// Response object
			JSONObject resp = new JSONObject() {{
				// Appends the contact info
				put("users",convertUsersAndContactsToJson(contacts));
			}};
			
			// Sends the response
			request.sendResponse(resp);
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
		}
		
	}
	
	/**
	 * Converts all given users and those users contact infos to a sturctured json-array
	 * @param contacts the contacts and their contac-infos
	 * @return an json array that holds the information
	 * @throws EntitySaveException if anything went wrong while saving the contacts
	 */
	private JSONArray convertUsersAndContactsToJson(Map<UserEntity, List<ContactInfoEntity>> contacts) throws EntitySaveException {
		// Array with all users
		JSONArray users = new JSONArray();
		
		// Iterates over all user and those contacts
		for(Entry<UserEntity, List<ContactInfoEntity>> user : contacts.entrySet()) {
			// The user object
			JSONObject userJson = new JSONObject();
			
			// Saves the user to the object
			user.getKey().save(userJson, UserEntity.ATTRIBUTE_LIST);
			
			// Appends the user infos
			userJson.put("contactinfo", this.concatInfoToJson(user.getValue()));
			
			// Appends the user
			users.put(userJson);
		}
		
		return users;
	}
	
	/**
	 * Simply converts the passed contact info list into an json-array
	 * Uses only the values from {@value #CONTACT_INFOS} to serialize the data
	 * 
	 * @param infos the infos as object 
	 * @return an json array that contains the saved data
	 * @throws EntitySaveException if anything went wrong while saving the data
	 */
	private JSONArray concatInfoToJson(List<ContactInfoEntity> infos) throws EntitySaveException {
		// The array
		JSONArray arr = new JSONArray();
		
		// Iterates over every contact info and add it to the array
		for(ContactInfoEntity i : infos) {
			// The object where the entity will be saved to
			JSONObject obj = new JSONObject();
			// Deserializes the object
			i.save(obj, ContactInfoEntity.ATTRIBUTE_LIST);
			// Appends the object
			arr.put(obj);
		}
		
		return arr;
	}

}
