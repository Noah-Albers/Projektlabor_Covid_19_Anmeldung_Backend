package de.noahalbers.plca.backend.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import de.noahalbers.plca.backend.Config;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;

public class PLCADatabase {
	
	// Reference to the program
	private PLCA plca = PLCA.getInstance();
	
	/**
	 * Tries to start a connection to the database
	 * @return the connection that has to be closed by the requester
	 * @throws SQLException if anything went wrong
	 */
	public Connection startConnection() throws SQLException {
		// Gets the config
		Config cfg = this.plca.getConfig();
		
		return DriverManager.getConnection(
			String.format(
				"jdbc:mysql://%s:%s/%s",
				cfg.get("db_host"),
				cfg.get("db_port"),
				cfg.get("db_databasename")
			),
			cfg.get("db_user"),
			cfg.get("db_password")
		);
	}
	
	/**
	 * Gets an admin entity by his id
	 * @param id the id of the admin
	 * @param con the connection to the database
	 * @return empty if no admin with that id got found, otherwise the admin
	 * @throws SQLException if anything went wrong with the connection
	 */
	public Optional<AdminEntity> getAdminById(int id,Connection con) throws SQLException {
		// Starts the statement
		PreparedStatement query = con.prepareStatement("SELECT * FROM admin WHERE id=?;");
		query.setInt(1, id);
		
		// Executes the query and gets the result
		ResultSet res = query.executeQuery();
		
		// Checks if no result got found
		if(!res.next())
			return Optional.empty();
		
		try {
			return Optional.of(new AdminEntity(res));
		}catch(Exception exc) {
			throw new SQLException(exc);
		}
	}
}
