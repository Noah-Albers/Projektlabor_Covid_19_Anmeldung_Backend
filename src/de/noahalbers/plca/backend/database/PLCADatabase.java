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
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.json.JSONException;

import com.mysql.cj.exceptions.RSAException;
import com.smattme.MysqlExportService;

import de.noahalbers.plca.backend.Config;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;
import de.noahalbers.plca.backend.database.entitys.TimespentEntity;
import de.noahalbers.plca.backend.database.entitys.UserEntity;
import de.noahalbers.plca.backend.database.exceptions.DuplicatedEntryException;
import de.noahalbers.plca.backend.database.exceptions.EntityLoadException;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.logger.Logger;

public class PLCADatabase {
	
	// Reference to the program
	private PLCA plca = PLCA.getInstance();

	// Reference to the logger
	private Logger log = this.plca.getLogger();
	
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
		
		this.log.debug("Starting database backup");
		
		// Gets the config
		Config cfg = this.plca.getConfig();

		// Generates the properties for the exporter
		Properties properties = new Properties();
		properties.setProperty(MysqlExportService.DB_USERNAME, cfg.get("db_user"));
		properties.setProperty(MysqlExportService.DB_PASSWORD, cfg.get("db_password"));
		properties.setProperty(MysqlExportService.JDBC_CONNECTION_STRING, this.generateConnectionString());

		// Gets the export service
		MysqlExportService s = new MysqlExportService(properties);
		try {
			this.log.debug("Starting the back, retrieving...");
			// Retrieves the backup
			s.export();
		} catch (ClassNotFoundException | IOException e) {
			this.log
			.debug("Backup failed")
			.critical(e);
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
		return DriverManager.getConnection(this.generateConnectionString(), cfg.get("db_user"), cfg.get("db_password"));
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
	 * Updates a given timespent on the database
	 * 
	 * @param con
	 *            the connection
	 * @param ts
	 *            the updated timespent entity
	 * @throws SQLException
	 *             if anything went wrong
	 * @throws EntitySaveException
	 *             this error should not occurre. If it does something went wrong in
	 *             the database
	 */
	public void updateTimespent(Connection con, TimespentEntity ts) throws SQLException, EntitySaveException {
		// Prepares the query
		try (PreparedStatement ps = con.prepareStatement(
				this.getUpdateQuery("timespent", TimespentEntity.ID, TimespentEntity.DB_ENTRY_LIST))) {
			// Inserts the values
			ts.save(ps, TimespentEntity.DB_ENTRY_LIST);
			// Inserts the primary value
			ps.setInt(TimespentEntity.DB_ENTRY_LIST.length + 1, ts.id);

			// Executes the statement
			ps.execute();
		}
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
				this.getInsertQuery("timespent", TimespentEntity.DB_ENTRY_LIST), Statement.RETURN_GENERATED_KEYS)) {
			// Inserts all values
			ts.save(ps, TimespentEntity.DB_ENTRY_LIST);
			// Creates the user
			ps.executeUpdate();

			// Gets the id
			ResultSet rs = ps.getGeneratedKeys();
			rs.next();

			// Appends the id to the admin
			ts.id = rs.getInt(1);
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
			ResultSet res = ps.executeQuery();

			// Checks if no last timespent got found
			if (!res.next())
				return Optional.empty();

			// Parses the timespent entity
			TimespentEntity ts = new TimespentEntity();
			ts.load(res, TimespentEntity.DB_ENTRY_LIST);

			return Optional.of(ts);
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
		try (PreparedStatement ps = con.prepareStatement(this.getInsertQuery("user", UserEntity.DB_ENTRY_LIST),
				Statement.RETURN_GENERATED_KEYS)) {

			// Inserts all values
			user.save(ps, UserEntity.DB_ENTRY_LIST);
			System.out.println(ps);
			// Creates the user
			ps.executeUpdate();

			// Gets the id
			ResultSet rs = ps.getGeneratedKeys();
			rs.next();

			// Appends the id to the admin
			user.id = rs.getInt(1);
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new DuplicatedEntryException(e);
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
			ps.setInt(3, Integer.valueOf(this.plca.getConfig().get("autologout_after_time")));

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
		Timestamp ts = new Timestamp(
				System.currentTimeMillis() + Long.valueOf(this.plca.getConfig().get("autodelete_time")));
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
	 * Grabs all users (Simplified profiles) from the database
	 * 
	 * @param con
	 *            the connection to the database
	 * @return all users from that are registered inside the database (Simple
	 *         profiles)
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public SimpleUserEntity[] getSimpleUsersFromDatabase(Connection con) throws SQLException {
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
				sue.load(res, SimpleUserEntity.DB_ENTRY_LIST);
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
				adm.load(res, AdminEntity.DB_ENTRY_LIST);

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
	public Entry<SimpleUserEntity, TimespentEntity> getUserByRFID(String rfid, Connection con) throws SQLException {
		
		// The user that shall be found
		SimpleUserEntity user = new SimpleUserEntity();
		
		// Prepares the select query for the user
		try (PreparedStatement ps = con.prepareStatement(
				this.getSelectQuery("user", UserEntity.RFID + "=?", SimpleUserEntity.DB_ENTRY_LIST))) {
			// Inserts the values
			ps.setString(1, rfid);
			// Executes the query
			ResultSet res = ps.executeQuery();
			
			// Checks if no user got found
			if(!res.next())
				return new AbstractMap.SimpleEntry<>(null,null);
			
			// Tries to parse the user
			user.load(res, SimpleUserEntity.DB_ENTRY_LIST);
		} catch (EntityLoadException e) {
			throw new SQLException(e);
		}
		
		// Loads the last open timespent of that user
		Optional<TimespentEntity> ts = this.getLastOpenTimespent(con, user.id);
		
		// Returns the entry for the user and optionally the timespent
		return new AbstractMap.SimpleEntry<>(user,ts.isPresent() ? ts.get() : null);
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
