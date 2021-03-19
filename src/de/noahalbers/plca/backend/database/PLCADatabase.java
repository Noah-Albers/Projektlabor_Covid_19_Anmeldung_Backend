package de.noahalbers.plca.backend.database;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.json.JSONException;

import com.mysql.cj.exceptions.RSAException;
import com.smattme.MysqlExportService;

import de.noahalbers.plca.backend.Config;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;

public class PLCADatabase {

	// Reference to the program
	private PLCA plca = PLCA.getInstance();
	
	/**
	 * Generates a connection string for the database
	 */
	public String generateConnectionString() {
		Config cfg = this.plca.getConfig();
		
		return String.format(
			"jdbc:mysql://%s:%s/%s",
			cfg.get("db_host"),
			cfg.get("db_port"),
			cfg.get("db_databasename")
		);
	}
	
	/**
	 * Creates a backup of the current database
	 * @return a string that can be inserted as list of statements and will recreate the backup of the database
	 * @throws SQLException if anything went wrong while grabbing the database
	 */
	public String requestDatabaseBackup() throws SQLException {
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
			// Retrieves the backup
			s.export();
		} catch (ClassNotFoundException | IOException e) {
			throw new SQLException(e);
		}
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
		return DriverManager.getConnection(this.generateConnectionString(),cfg.get("db_user"), cfg.get("db_password"));
	}

	/**
	 * Logs out all users that have been logged in to long (Config value)
	 * @param con the connection
	 * @throws SQLException if anything went wrong
	 */
	public void doAutologoutUsers(Connection con) throws SQLException{
		// Gets the current timestamp
		Timestamp current = new Timestamp(System.currentTimeMillis());
		// Creates the query
		try(PreparedStatement ps = con.prepareStatement("UPDATE `timespent` SET `stop`=?,`enddisconnect`=1 WHERE `stop` IS NULL AND TIMESTAMPDIFF(hour,start,?) >= ?;")){
			// Inserts all values
			ps.setTimestamp(1, current);
			ps.setTimestamp(2, current);
			ps.setInt(3, Integer.valueOf(this.plca.getConfig().get("autologout_after_time")));
			
			// Executes the statement
			if(!ps.execute())
				throw new SQLException("Failed to send query.");
		}
	}
	
	/**
	 * Deletes old accounts that have not been used in the specified amount of time (using the config)
	 * @param con the connection
	 * @throws SQLException if anything went wrong with the connection
	 */
	public void doAutoDeleteAccounts(Connection con) throws SQLException {
		
		// Calculates the timestamp before which old accounts should be deleted
		Timestamp ts = new Timestamp(System.currentTimeMillis() + Long.valueOf(this.plca.getConfig().get("autodelete_time")));
		// Creates the statement
		try(Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)){			
			// Adds the batches to delete
			stmt.addBatch("CREATE TEMPORARY TABLE IF NOT EXISTS `oldUsers` AS (SELECT `user`.`id` as `id` FROM `timespent` `o` LEFT JOIN `timespent` `b` ON `o`.`userid` = `b`.`userid` AND `o`.`stop` < `b`.`stop` right outer JOIN `user` ON `o`.`userid` = `user`.`id` WHERE(`b`.`stop` IS NULL AND `o`.`stop` IS NOT NULL AND `o`.`stop` AND `o`.`stop` < '"+ts.toString()+"') or `b`.`userid` IS NULL);");
			stmt.addBatch("DELETE FROM `user` where `autodeleteaccount` and `id` IN (SELECT `id` FROM `oldUsers`) and `createdate` < '"+ts.toString()+"';");
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
	 * @return all users from that are registered inside the database (Simple profiles)
	 * @throws SQLException
	 *             if anything went wrong with the connection
	 */
	public SimpleUserEntity[] getSimpleUsersFromDatabase(Connection con) throws SQLException {
		try(
			// Starts the statement
			PreparedStatement query = con.prepareStatement("SELECT id,firstname,lastname FROM user;");
			// Executes the query and gets the result
			ResultSet res = query.executeQuery()){	
			
			// Holds all found users
			List<SimpleUserEntity> users = new ArrayList<>();
			
			// Grabs all users
			while (res.next()) {
				// Creates the user
				SimpleUserEntity sue = new SimpleUserEntity();
				// Imports all parameters
				this.importValuesIntoObject(sue, res);
				// Appends the object to the list
				users.add(sue);
			}
			
			return (SimpleUserEntity[]) users.toArray(new SimpleUserEntity[users.size()]);
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
		try(PreparedStatement query = con.prepareStatement("SELECT * FROM admin WHERE id=?;")){
			// Appends the parameters
			query.setInt(1, id);
			
			// Executes the query and gets the result
			try(ResultSet res = query.executeQuery()){				
				// Checks if no result got found
				if (!res.next())
					return Optional.empty();
				
				// Creates an admin entity
				AdminEntity adm = new AdminEntity(res);
				
				// Imports all values
				this.importValuesIntoObject(adm, res);
				
				return Optional.of(adm);
			}
		} catch(JSONException | RSAException e) {
			throw new SQLException(e);
		}

	}

	/**
	 * Takes an object and import all its properties from the database. Used @DBInfo
	 * to select a propertie that shall be import or/and exportable.
	 * 
	 * @param object
	 *            the object to modify
	 * @param result
	 *            the resultset from the database
	 * @throws SQLException
	 *             if a column from the result is not valid
	 */
	private <T> void importValuesIntoObject(T object, ResultSet result) throws SQLException {
		// Iterates over all field from the object
		for (Field f : object.getClass().getDeclaredFields()) {
			// Checks if the field is final (Not set-able)
			if (Modifier.isFinal(f.getModifiers()))
				continue;

			// Gets the name of the field
			DBInfo info = f.getDeclaredAnnotation(DBInfo.class);

			// Checks if no name was given
			if (info == null)
				continue;

			// Checks if the field can't be imported
			if (!info.importable())
				continue;

			// Makes the field access-able
			f.setAccessible(true);
			// Imports the value
			try {
				f.set(object, result.getObject(info.value()));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO: better logging
				e.printStackTrace();
				throw new SQLException("Failed to access the object.");
			}
		}
	}
}
