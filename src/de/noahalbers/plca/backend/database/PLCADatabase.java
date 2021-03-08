package de.noahalbers.plca.backend.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import de.noahalbers.plca.backend.Config;

public class PLCADatabase {
	
	/**
	 * Tries to start a connection to the database
	 * @return the connection that has to be closed by the requester
	 * @throws SQLException if anything went wrong
	 */
	public Connection startConnection() throws SQLException {
		return DriverManager.getConnection(
			String.format(
				"jdbc:mysql://%s:%s/%s",
				Config.getInstance().get("db_host"),
				Config.getInstance().get("db_port"),
				Config.getInstance().get("db_databasename")
			),
			Config.getInstance().get("db_user"),
			Config.getInstance().get("db_password")
		);
	}
	
}
