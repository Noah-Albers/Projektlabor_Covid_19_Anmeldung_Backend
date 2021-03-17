package de.noahalbers.plca.backend.database;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.noahalbers.plca.backend.Config;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;

public class PLCADatabase {

	// Reference to the program
	private PLCA plca = PLCA.getInstance();

	/**
	 * Tries to start a connection to the database
	 * 
	 * @return the connection that has to be closed by the requester
	 * @throws SQLException
	 *             if anything went wrong
	 */
	public Connection startConnection() throws SQLException {
		// Gets the config
		Config cfg = this.plca.getConfig();

		return DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s", cfg.get("db_host"),
				cfg.get("db_port"), cfg.get("db_databasename")), cfg.get("db_user"), cfg.get("db_password"));
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
		// Starts the statement
		PreparedStatement query = con.prepareStatement("SELECT id,firstname,lastname FROM user;");

		// Executes the query and gets the result
		ResultSet res = query.executeQuery();

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
		PreparedStatement query = con.prepareStatement("SELECT * FROM admin WHERE id=?;");
		query.setInt(1, id);

		// Executes the query and gets the result
		ResultSet res = query.executeQuery();

		// Checks if no result got found
		if (!res.next())
			return Optional.empty();

		try {
			// Creates an admin entity
			AdminEntity adm = new AdminEntity(res);

			// Imports all values
			this.importValuesIntoObject(adm, res);

			return Optional.of(adm);
		} catch (Exception exc) {
			throw new SQLException(exc);
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
