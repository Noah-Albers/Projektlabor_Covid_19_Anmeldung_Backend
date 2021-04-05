package de.noahalbers.plca.backend.database.exceptions;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import de.noahalbers.plca.backend.database.PLCADatabase;

public class DuplicatedEntryException extends SQLException{
	private static final long serialVersionUID = 1736573624006882409L;

	// Name of the duplicated field
	private String name;
	
	public DuplicatedEntryException(SQLIntegrityConstraintViolationException exception) {
		this.name = PLCADatabase.getDuplicatedEntry(exception);
	}
	
	public String getFieldName() {
		return this.name;
	}
}
