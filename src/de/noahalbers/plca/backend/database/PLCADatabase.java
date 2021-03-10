package de.noahalbers.plca.backend.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import de.noahalbers.plca.backend.Config;
import de.noahalbers.plca.backend.PLCA;

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
				cfg.getString("db_host"),
				cfg.getString("db_port"),
				cfg.getString("db_databasename")
			),
			cfg.getString("db_user"),
			cfg.getString("db_password")
		);
	}
	
}
