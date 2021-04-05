package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.UserEntity;
import de.noahalbers.plca.backend.database.exceptions.DuplicatedEntryException;
import de.noahalbers.plca.backend.database.exceptions.EntityLoadException;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;

public class RegisterUserRequest extends RequestHandler{
	
	/**
	 * Responses:
	 * 	Errors:
	 * 		database: Backend failed to establish a valid database connection
	 * 		user: Client did not send a valid user (Could not be parsed, please check requirements)
	 * 		unknown: an unknown error occurre. This should not happen. Please try again.
	 * 		dup.name: A user with that name already exists
	 * 		dup.rfid: A user with the rfid already exists
	 * 
	 * 	Success:
	 * 		id: holds the users id
	 * 	
	 * Request:
	 * 		All entrys from REGISTER_ENTRYS and optionally those from OPTIONAL_REGISTER_ENTRYS
	 * 
	 */
	
	// All entry's that the user has to pass
	private static final String[] REGISTER_ENTRYS = {
		UserEntity.AUTODELETE,
		UserEntity.HOUSE_NUMBER,
		UserEntity.LOCATION,
		UserEntity.POSTAL_CODE,
		UserEntity.STREET,
		UserEntity.FIRSTNAME,
		UserEntity.LASTNAME
	};
	
	// All optional entry's that the user can pass
	private static final String[] OPTIONAL_REGISTER_ENTRYS = {
		UserEntity.EMAIL,
		UserEntity.TELEPHONE,
		UserEntity.RFID
	};
	
	@Override
	public int getRequiredPermissions() {
		return Permissions.DEFAULT_LOGIN;
	}

	@Override
	public void execute(Request request) throws IOException {
		try {
			// Gets the send data
			JSONObject m = request.getMessage();
	
			// Holds the new user
			UserEntity user = new UserEntity();
			// Loads the user
			user.load(m, REGISTER_ENTRYS, OPTIONAL_REGISTER_ENTRYS);
			// Sets the values
			user.createdate=new Timestamp(System.currentTimeMillis());
			
			// Checks if an error appeared
			this.database.registerUser(request.startDatabaseConnection(), user);
			
			// Sends back that the request was successful
			request.sendResponse(new JSONObject() {{
				this.put("id", user.id);
			}});

			this.logger
			.debug(request+"Registered new user")
			.critical(request+"Id="+user.id);
		} catch(DuplicatedEntryException e) {
			
			// Checks the name
			switch(e.getFieldName()) {
				case "uq_name":
					this.logger
					.debug(request+"Username already exists");
					// Sends a duplicate exception for the name
					request.sendError("dup.name");
					break;
				case "rfid":
					this.logger
					.debug(request+"RFID already exists");
					// Sends a duplicated exception for the rfid
					request.sendError("dup.rfid");
					break;
			}
			
			// Unknown database error
			this.sendErrorDatabase(request, e);
			
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
		} catch(EntityLoadException e) {
			// Log
			this.logger.debug(request+"has not send a valid user (could not be parsed)");
			
			// Sends the error
			request.sendError("user");
		}
	}
}
