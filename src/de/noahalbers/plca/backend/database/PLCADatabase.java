package de.noahalbers.plca.backend.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.json.JSONException;

import com.mysql.cj.exceptions.RSAException;
import com.smattme.MysqlExportService;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.config.Config;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.database.entitys.ContactInfoEntity;
import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;
import de.noahalbers.plca.backend.database.entitys.TimespentEntity;
import de.noahalbers.plca.backend.database.entitys.UserEntity;
import de.noahalbers.plca.backend.database.exceptions.DuplicatedEntryException;
import de.noahalbers.plca.backend.database.exceptions.EntityLoadException;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.logger.Logger;
import de.noahalbers.plca.backend.util.Nullable;

public class PLCADatabase {

	// Reference to the program
	private PLCA plca = PLCA.getInstance();

	// Reference to the logger
	private Logger log = new Logger("PLCADatabase");

	/**
	 * Generates a connection string for the database
	 */
	public String generateConnectionString() {
		// Reference to the config
		Config cfg = this.plca.getConfig();

		return String.format("jdbc:mysql://%s:%s/%s", cfg.get("db_host"), cfg.get("db_port"),
				cfg.get("db_databasename"));
	}

	/**
	 * Creates a backup of the current database
	 * 
	 * @return a string that can be inserted as list of statements and will recreate
	 *         the backup of the database
	 * @throws SQLException
	 *             if anything went wrong while grabbing the database
	 */
	public String requestDatabaseBackup() throws SQLException {

		this.log.debug("Starting backup");

		// Gets the config
		Config cfg = this.plca.getConfig();

		// Gets the password
		String pw = cfg.getUnsafe("db_password");
		if (pw.equals("*"))
			pw = "";

		// Generates the properties for the exporter
		Properties properties = new Properties();
		properties.setProperty(MysqlExportService.DB_USERNAME, cfg.getUnsafe("db_user"));
		properties.setProperty(MysqlExportService.DB_PASSWORD, pw);
		properties.setProperty(MysqlExportService.JDBC_CONNECTION_STRING, this.generateConnectionString());

		// Gets the export service
		MysqlExportService s = new MysqlExportService(properties);
		try {
			this.log.debug("Retrieving...");
			// Retrieves the backup
			s.export();
		} catch (ClassNotFoundException | IOException e) {
			this.log.debug("Backup failed").critical(e);
			throw new SQLException(e);
		}

		this.log.debug("Backup received successfully");

		// Returns the generated backup as a string
		return s.getGeneratedSql();
	}

	/**
	 * Tries to start a connection to the database
	 * 
	 * @return the connection that has to be closed by the requester
	 * @throws SQLException
	 *             if anything went wrong
	 */
	public Connection startConnection() throws SQLException {
		Config cfg = this.plca.getConfig();

		// Gets the password
		String pw = cfg.getUnsafe("db_password");
		if (pw.equals("*"))
			pw = "";

		return DriverManager.getConnection(this.generateConnectionString(), cfg.getUnsafe("db_user"), pw);
	}

	/**
	 * Checks if a user with the given id is registered
	 * 
	 * @param con
	 *            the connection
	 * @param userId
	 *            the id of the user that shall be checked
	 * @return if a user with the given id exists
	 * @throws SQLException
	 *             if anything went wrong
	 */
	public boolean doesUserExists(Connection con, int userId) throws SQLException {
		// Prepares the query
		try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM `user` WHERE `id`=?;")) {
			// Appends the values
			ps.setInt(1, userId);

			// Returns if any user got found
			return ps.executeQuery().next();
		}
	}

	/**
	 * Updates a given admin on the database
	 * 
	 * @param con
	 *            the connection
	 * @param entity
	 *            the updated admin entity
	 * @throws SQLException
	 *             if anything went wrong
	 * @throws EntitySaveException
	 *             this error should not occurre
	 */
	public void updateAdmin(Connection con, AdminEntity entity) throws SQLException, EntitySaveException {
		// Prepares the query
		try (PreparedStatement ps = con
				.prepareStatement(this.getUpdateQuery("admin", AdminEntity.ID, AdminEntity.ATTRIBUTE_LIST))) {
			// Inserts the values
			entity.save(ps, AdminEntity.ATTRIBUTE_LIST);
			// Inserts the primary value
			ps.setInt(AdminEntity.ATTRIBUTE_LIST.length + 1, entity.id);
			
			// Executes the statement
			ps.execute();
		}
	}

	/**
	 * Freezes an admin's account
	 * 
	 * @param con
	 *            the connection to use
	 * @param adminId
	 *            the id of the admin of which the account should be frozen
	 * @throws SQLException
	 *             if anything went wrong with the database
	 */
	public void freezeAdminAccount(Connection con, int adminId) throws SQLException {
		try (PreparedStatement ps = con
				.prepareStatement("UPDATE `admin` SET `" + AdminEntity.IS_FROZEN + "`=1 WHERE`"+AdminEntity.ID+"`=?;")) {
			// Set the value
			ps.setInt(1, adminId);

			// Executes the update
			ps.executeUpdate();
		}
	}

	/**
	 * Updates the user on the database
	 * 
	 * @param con
	 *            the connection to use
	 * @param entity
	 *            the user with the updates (Id will be used to select the user)
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 * @throws EntitySaveException
	 *             this error should not occurre
	 */
	public void updateUser(Connection con, UserEntity entity) throws SQLException, EntitySaveException {
		// Prepares the query
		try (PreparedStatement ps = con
				.prepareStatement(this.getUpdateQuery("user", SimpleUserEntity.ID, UserEntity.ATTRIBUTE_LIST))) {
			// Inserts the values
			entity.save(ps, UserEntity.ATTRIBUTE_LIST);
			// Inserts the primary value
			ps.setInt(UserEntity.ATTRIBUTE_LIST.length + 1, entity.id);

			// Executes the statement
			ps.execute();
		}
	}

	/**
	 * Updates a given timespent on the database
	 * 
	 * @param con
	 *            the connection
	 * @param entity
	 *            the updated timespent entity
	 * @throws SQLException
	 *             if anything went wrong
	 * @throws EntitySaveException
	 *             this error should not occurre
	 */
	public void updateTimespent(Connection con, TimespentEntity entity) throws SQLException, EntitySaveException {
		// Prepares the query
		try (PreparedStatement ps = con.prepareStatement(
				this.getUpdateQuery("timespent", TimespentEntity.ID, TimespentEntity.ATTRIBUTE_LIST))) {
			// Inserts the values
			entity.save(ps, TimespentEntity.ATTRIBUTE_LIST);
			// Inserts the primary value
			ps.setInt(TimespentEntity.ATTRIBUTE_LIST.length + 1, entity.id);

			// Executes the statement
			ps.execute();
		}
	}

	// Uses by getContactInfosForUser to determin what infos are required and
	// optional
	private static String[] REQUIRED_CONTACT_ENTITYS = { UserEntity.ID, UserEntity.FIRSTNAME, UserEntity.LASTNAME,
			UserEntity.POSTAL_CODE, UserEntity.LOCATION, UserEntity.STREET, UserEntity.HOUSE_NUMBER };
	private static String[] OPTIONAL_CONTACT_ENTITYS = { UserEntity.EMAIL, UserEntity.TELEPHONE };

	/**
	 * Searches all contacts and contact-infos (time and date) from the database
	 * that a user had contact with
	 * 
	 * @param con
	 *            - the connection to use
	 * @param userid
	 *            - the id of the infected user.
	 * @param afterDate
	 *            - the date that shall be used to determine old entry's. Contacts
	 *            before this date wont be counted
	 * @param marginTime
	 *            - the amount of time (minutes) that should be added as a margin to
	 *            the logout time of a user as the time where the aerosols are still
	 *            present
	 * @return a list with users that had contact and a list for every user with
	 *         their corresponding contact times (contact infos)
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public Map<UserEntity, List<ContactInfoEntity>> getContactInfosForUser(Connection con, int userid,
			Timestamp afterDate, int marginTime) throws SQLException {

		// If anything went wrong while asyncly requesting the users this value will be
		// set
		AtomicReference<Exception> optError = new AtomicReference<>();

		// Received user are stored here
		Map<UserEntity, List<ContactInfoEntity>> grabbedUsers = new HashMap<>();
		// Received contact-infos are stored here
		List<ContactInfoEntity> grabbedInfos = new ArrayList<>();

		// Prepares the threaded runnable to grab all users that had contact
		Runnable userSelection = () -> {
			// Prepares the query to get all infected users
			try (PreparedStatement ps = con.prepareStatement(
					"SELECT DISTINCT u.id, u.firstname, u.lastname, u.postalcode, u.location, u.street, u.housenumber, u.telephone, u.email FROM timespent i JOIN timespent c ON i.userid != c.userid AND ADDTIME(CASE WHEN i.stop IS NULL THEN UTC_TIMESTAMP() ELSE i.stop END, ? ) >= c.start AND i.start <=( CASE WHEN c.stop IS NULL THEN UTC_TIMESTAMP() ELSE c.stop END) JOIN user u ON u.id=c.userid WHERE i.userid = ? AND i.stop > ?;")) {
				// Sets the values
				ps.setInt(1, marginTime * 60);
				ps.setInt(2, userid);
				ps.setTimestamp(3, afterDate);

				// Executes the query
				try(ResultSet res = ps.executeQuery()){					
					// Loads all passed entitys
					while (res.next()) {
						// Creates the user
						UserEntity user = new UserEntity();
						// Loads the users values
						user.load(res, REQUIRED_CONTACT_ENTITYS, OPTIONAL_CONTACT_ENTITYS);
						// Adds the user
						grabbedUsers.put(user, new ArrayList<>());
					}
				}

			} catch (SQLException | EntityLoadException e) {
				// Passes on the error
				optError.set(e);
			}
		};

		// Prepares the threaded runnable to grab all contact infos for the time a user
		// had contact with another user
		Runnable contactSelection = () -> {
			// Prepares the query to grab all contact infos
			try (PreparedStatement ps = con.prepareStatement(
					"SELECT i.start AS 'istart', (CASE WHEN i.stop IS NULL THEN UTC_TIMESTAMP() ELSE i.stop END) AS 'istop', c.userid AS 'cid', c.start AS 'cstart', (CASE WHEN c.stop IS NULL THEN UTC_TIMESTAMP() ELSE c.stop END) AS 'cStop' FROM timespent i JOIN timespent c ON i.userid != c.userid AND ADDTIME(CASE WHEN i.stop IS NULL THEN UTC_TIMESTAMP() ELSE i.stop END, ?) >= c.start AND i.start <= (CASE WHEN c.stop IS NULL THEN UTC_TIMESTAMP() ELSE c.stop END) WHERE i.userid = ? AND i.stop > ?;")) {
				// Sets the values
				ps.setInt(1, marginTime * 60);
				ps.setInt(2, userid);
				ps.setTimestamp(3, afterDate);

				// Executes the query
				try(ResultSet res = ps.executeQuery()){					
					// Interprets the values
					while (res.next()) {
						// Creates the user
						ContactInfoEntity user = new ContactInfoEntity();
						// Loads the users values
						user.load(res, ContactInfoEntity.ATTRIBUTE_LIST);
						// Adds the user
						grabbedInfos.add(user);
					}
				}

			} catch (SQLException | EntityLoadException e) {
				optError.set(e);
			}
		};

		// Creates the threads
		Thread userThread = new Thread(userSelection);
		Thread contactThread = new Thread(contactSelection);

		// Starts the threads
		userThread.start();
		contactThread.start();

		// Waits for both threads to finish
		try {
			userThread.join();
			contactThread.join();
		} catch (InterruptedException e) {
		}

		// Gets a possible error
		Exception ex = optError.get();

		// Check if an error occurred
		if (ex != null)
			throw ex instanceof SQLException ? (SQLException) ex : new SQLException(ex);

		// Gets the users as a keyset
		Set<UserEntity> rawUsers = grabbedUsers.keySet();

		try {
			// Iterates over all contact-infos
			grabbedInfos.forEach(i -> {
				// Searches the user with the corresponding user-id (for the contact)
				UserEntity contact = rawUsers.stream().filter(x -> x.id == i.contactID).findFirst().get();
				// Appends the contact-info
				grabbedUsers.get(contact).add(i);
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw new SQLException(e);
		}

		return grabbedUsers;
	}

	/**
	 * Creates a new timespent on the database
	 * 
	 * @param con
	 *            the connection
	 * @param ts
	 *            the newly created timespententity. Should not contain an id, as
	 *            that is the primary key and will be set after creation by the
	 *            database.
	 * @throws SQLException
	 *             if anything went wrong with the database
	 * @throws EntitySaveException
	 *             this error should not occurre. If it does it is an unknown error
	 * @throws IllegalStateException
	 *             if the id is duplicated
	 */
	public void createTimespent(Connection con, TimespentEntity ts)
			throws IllegalStateException, SQLException, EntitySaveException {

		// Checks if the id is set
		if (ts.id != null)
			throw new IllegalStateException(TimespentEntity.ID);

		// Prepares the query
		try (PreparedStatement ps = con.prepareStatement(
				this.getInsertQuery("timespent", TimespentEntity.ATTRIBUTE_LIST), Statement.RETURN_GENERATED_KEYS)) {
			// Inserts all values
			ts.save(ps, TimespentEntity.ATTRIBUTE_LIST);
			// Creates the user
			ps.executeUpdate();

			// Gets the id
			try(ResultSet rs = ps.getGeneratedKeys()){
				// Gets the result for the new id
				rs.next();
				
				// Appends the id to the admin
				ts.id = rs.getInt(1);
			}
		}
	}
	
	/**
	 * Tries to load the last open timespent entity (that has not been logged out)
	 * 
	 * @param con
	 *            the connection
	 * @param userId
	 *            the user of which the last entity is searched.
	 * @return empty if there is no last timespent entity; otherwise that timespent
	 *         entity
	 * @throws SQLException
	 *             if anything went wrong with the connection.
	 */
	public Optional<TimespentEntity> getLastOpenTimespent(Connection con, int userId) throws SQLException {

		// Creates the statement
		try (PreparedStatement ps = con
				.prepareStatement("SELECT * FROM `timespent` WHERE `userid`=? AND `stop` IS NULL LIMIT 1;")) {
			ps.setInt(1, userId);

			// Executes the query
			try(ResultSet res = ps.executeQuery()){				
				// Checks if no last timespent got found
				if (!res.next())
					return Optional.empty();
				
				// Parses the timespent entity
				TimespentEntity ts = new TimespentEntity();
				ts.load(res, new String[]{
						TimespentEntity.ID,
						TimespentEntity.START_TIME, 
						TimespentEntity.END_DISCONNECTED, 
						TimespentEntity.USER_ID
				});
				
				return Optional.of(ts);
			}
		} catch (EntityLoadException e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Registers a new user
	 * 
	 * @param con
	 *            the connection that should be used
	 * @param user
	 *            the user that should be created.
	 * @return ErrorCode.NONE if everything went right; otherwise the error-code if
	 *         the problem.
	 * @throws SQLException
	 *             if anything went wrong with the database
	 * @throws IllegalStateException
	 *             if the id is set (which should not be the case as it's generated
	 *             by the database and returned)
	 * @throws DuplicatedEntryException
	 *             if any value is duplicated (In this context only the combination
	 *             of the first and lastname should not be duplicated and will lead
	 *             to this error)
	 * @throws EntitySaveException
	 *             this error should not occurre. If it does it's an unknown error.
	 */
	public void registerUser(Connection con, UserEntity user)
			throws IllegalStateException, DuplicatedEntryException, SQLException, EntitySaveException {

		// Checks if the id is set and therefore could collide with an existing user
		if (user.id != null)
			throw new IllegalStateException(SimpleUserEntity.ID);

		// Prepares the statement
		try (PreparedStatement ps = con.prepareStatement(this.getInsertQuery("user", UserEntity.ATTRIBUTE_LIST),
				Statement.RETURN_GENERATED_KEYS)) {

			// Inserts all values
			user.save(ps, UserEntity.ATTRIBUTE_LIST);
			
			// Creates the user
			ps.executeUpdate();

			// Gets the id
			try(ResultSet rs = ps.getGeneratedKeys()){
				// Gets the id of the new row
				rs.next();
				
				// Appends the id to the admin
				user.id = rs.getInt(1);
			}

		} catch (SQLIntegrityConstraintViolationException e) {
			throw new DuplicatedEntryException(e);
		}

	}

	/**
	 * Logs out all users that are still logged in.
	 * 
	 * @param con
	 *            the connection to use
	 * @return how many user got logged out
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public int logoutAllUsers(Connection con) throws SQLException {
		// Creates the query
		try (PreparedStatement ps = con.prepareStatement("UPDATE `timespent` SET `stop`=? WHERE `stop` IS NULL;")) {
			ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));

			// Sends the update
			return ps.executeUpdate();
		}
	}

	/**
	 * Logs out all users that have been logged in to long (Config value)
	 * 
	 * @param con
	 *            the connection
	 * @throws SQLException
	 *             if anything went wrong
	 */
	public void doAutologoutUsers(Connection con) throws SQLException {
		// Gets the current timestamp
		Timestamp current = new Timestamp(System.currentTimeMillis());
		// Creates the query
		try (PreparedStatement ps = con.prepareStatement(
				"UPDATE `timespent` SET `stop`=?,`enddisconnect`=1 WHERE `stop` IS NULL AND TIMESTAMPDIFF(hour,start,?) >= ?;")) {
			// Inserts all values
			ps.setTimestamp(1, current);
			ps.setTimestamp(2, current);
			ps.setInt(3, this.plca.getConfig().getUnsafe("autologout_after_time"));

			// Executes the statement
			if (!ps.execute())
				throw new SQLException("Failed to send query.");
		}
	}

	/**
	 * Deletes old accounts that have not been used in the specified amount of time
	 * (using the config)
	 * 
	 * @param con
	 *            the connection
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public void doAutoDeleteAccounts(Connection con) throws SQLException {

		// Calculates the timestamp before which old accounts should be deleted
		Timestamp ts = new Timestamp(System.currentTimeMillis() + (Long) this.plca.getConfig().get("autodelete_time"));
		// Creates the statement
		try (Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			// Adds the batches to delete
			stmt.addBatch(
					"CREATE TEMPORARY TABLE IF NOT EXISTS `oldUsers` AS (SELECT `user`.`id` as `id` FROM `timespent` `o` LEFT JOIN `timespent` `b` ON `o`.`userid` = `b`.`userid` AND `o`.`stop` < `b`.`stop` right outer JOIN `user` ON `o`.`userid` = `user`.`id` WHERE(`b`.`stop` IS NULL AND `o`.`stop` IS NOT NULL AND `o`.`stop` AND `o`.`stop` < '"
							+ ts.toString() + "') or `b`.`userid` IS NULL);");
			stmt.addBatch(
					"DELETE FROM `user` where `autodeleteaccount` and `id` IN (SELECT `id` FROM `oldUsers`) and `createdate` < '"
							+ ts.toString() + "';");
			stmt.addBatch("DROP TABLE IF EXISTS `oldUsers`;");

			// Executes the batch
			stmt.executeBatch();
		}

	}

	/**
	 * Gets a user by his id
	 * 
	 * @param con
	 *            the connection to use
	 * @param userId
	 *            the id of the user
	 * @return the user if pressent; otherwise empty
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public Optional<UserEntity> getUser(Connection con, int userId) throws SQLException {

		// Prepares the query
		try (PreparedStatement ps = con.prepareStatement("SELECT * FROM user WHERE `id`=?;")) {
			// Sets the values
			ps.setInt(1, userId);

			// Executes the query
			try(ResultSet res = ps.executeQuery()){				
				
				// Checks if no user got found
				if (!res.next())
					return Optional.empty();
				
				try {
					// Parses the user
					UserEntity user = new UserEntity();
					user.load(res,UserEntity.REQUIRED_ATTRIBUTE_LIST,UserEntity.OPTIONAL_ATTRIBUTE_LIST);
					
					return Optional.of(user);
				} catch (EntityLoadException e) {
					throw new SQLException(e);
				}
			}
		}
	}

	/**
	 * Grabs all users (Simplified profiles) from the database
	 * 
	 * @param con
	 *            the connection to the database
	 * @return all users from that are registered inside the database (Simple
	 *         profiles)
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public SimpleUserEntity[] getSimpleUsers(Connection con) throws SQLException {
		try (
			// Starts the statement
			PreparedStatement query = con.prepareStatement("SELECT id,firstname,lastname FROM user;");
			// Executes the query and gets the result
			ResultSet res = query.executeQuery()) {

			// Holds all found users
			List<SimpleUserEntity> users = new ArrayList<>();

			// Grabs all users
			while (res.next()) {
				// Creates the user
				SimpleUserEntity sue = new SimpleUserEntity();
				// Imports all parameters
				sue.load(res, SimpleUserEntity.ATTRIBUTE_LIST);
				// Appends the object to the list
				users.add(sue);
			}

			return (SimpleUserEntity[]) users.toArray(new SimpleUserEntity[users.size()]);
		} catch (EntityLoadException e) {
			throw new SQLException(e);
		}

	}

	/**
	 * Gets an admin entity by his id
	 * 
	 * @param id
	 *            the id of the admin
	 * @param con
	 *            the connection to the database
	 * @return empty if no admin with that id got found, otherwise the admin
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public Optional<AdminEntity> getAdminById(int id, Connection con) throws SQLException {
		// Starts the statement
		try (PreparedStatement query = con.prepareStatement("SELECT * FROM admin WHERE id=?;")) {
			// Appends the parameters
			query.setInt(1, id);

			// Executes the query and gets the result
			try (ResultSet res = query.executeQuery()) {
				// Checks if no result got found
				if (!res.next())
					return Optional.empty();

				// Creates an admin entity
				AdminEntity adm = new AdminEntity();
				// Loads all values from the result set
				adm.load(res,AdminEntity.REQUIRED_ATTRIBUTE_LIST,AdminEntity.OPTIONAL_ATTRIBUTE_LIST);

				return Optional.of(adm);
			}
		} catch (JSONException | RSAException | EntityLoadException e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Searches a user and if any open existing timespent is available that to. The
	 * search depends on the user's rfid that gets passed.
	 * 
	 * @param rfid
	 *            - the rfid of the user to search for
	 * @return if the user is not found, an empty entry with (null;null); otherwise
	 *         if the user is found but no corresponding timespententity that the
	 *         user and an empty value on the tuple (user;null); if both are found,
	 *         both (user;timespent)
	 */
	public Entry<SimpleUserEntity, TimespentEntity> getSimpleUserByRFID(String rfid, Connection con)
			throws SQLException {

		// The user that shall be found
		SimpleUserEntity user = new SimpleUserEntity();

		// Prepares the select query for the user
		try (PreparedStatement ps = con.prepareStatement(
				this.getSelectQuery("user", UserEntity.RFID + "=?", SimpleUserEntity.ATTRIBUTE_LIST))) {
			// Inserts the values
			ps.setString(1, rfid);
			// Executes the query
			try(ResultSet res = ps.executeQuery()){				
				// Checks if no user got found
				if (!res.next())
					return new AbstractMap.SimpleEntry<>(null, null);
				
				// Tries to parse the user
				user.load(res, SimpleUserEntity.ATTRIBUTE_LIST);
			}
		} catch (EntityLoadException e) {
			throw new SQLException(e);
		}

		// Loads the last open timespent of that user
		Optional<TimespentEntity> ts = this.getLastOpenTimespent(con, user.id);

		// Returns the entry for the user and optionally the timespent
		return new AbstractMap.SimpleEntry<>(user, ts.isPresent() ? ts.get() : null);
	}

	/**
	 * Generates a string that can be used as an update query for a prepared
	 * statement. Leaves questionmark's for all given entry's to fill by the
	 * prepared statement.
	 * 
	 * @param table
	 *            the table that should be used
	 * @param entrys
	 *            the entry's that are expected to be filled.
	 * @return the query as a string
	 */
	private String getUpdateQuery(String table, String primaryAttribute, String... entrys) {
		// Creates the query to update an entity
		return String.format("UPDATE `" + table + "` SET %s WHERE %s=?",
				Arrays.stream(entrys).map(i -> '`' + i + "`=?").collect(Collectors.joining(",")), primaryAttribute);
	}

	/**
	 * Generates a string that can be used as a select query for a prepared
	 * statement.
	 * 
	 * @param table
	 *            the table that should be used
	 * @param conditions
	 *            {@link Nullable} conditions that are applied behind the where
	 *            clause if given
	 * @param selectEntrys
	 *            the entry's that are expected to be returned by the select-query
	 * @return the query as a string
	 */
	private String getSelectQuery(String table, @Nullable String conditions, String... selectEntrys) {
		// Creates the query to select a number of entitys
		return String.format("SELECT %s FROM `%s` %s",
				Arrays.stream(selectEntrys).map(i -> '`' + i + "`").collect(Collectors.joining(",")), table,
				conditions == null ? "" : "WHERE " + conditions);
	}

	/**
	 * Generates a string that can be used as an insert query for a prepared
	 * statement. Leaves questionmark's for all given entry's to fill by the
	 * prepared statement.
	 * 
	 * @param table
	 *            the table that should be used
	 * @param entrys
	 *            the entry's that are expected to be filled.
	 * @return the query as a string
	 */
	private String getInsertQuery(String table, String... entrys) {
		// Creates the query to create an entity
		return String.format("INSERT INTO `" + table + "` (%s) VALUES (%s)", String.join(",", entrys),
				String.join(",", Arrays.stream(entrys).map(i -> "?").collect(Collectors.joining(","))));
	}

	/**
	 * Takes an duplicated exception and returns the exact name of the field or
	 * combined field that is duplicated.
	 * 
	 * @param exception
	 *            - the {@link SQLException} that got thrown because of a duplicated
	 *            value
	 * @return the name of the field or combined field that is duplicated
	 */
	public static String getDuplicatedEntry(SQLIntegrityConstraintViolationException exception) {

		// Shorts the message
		String msg = exception.getMessage();

		// Gets the index
		int index = msg.lastIndexOf("'", msg.lastIndexOf("'") - 1);

		// Returns the substring
		return msg.substring(index + 1, msg.length() - 1);
	}
}
